package com.mockly.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockly.api.websocket.SessionEventPublisher;
import com.mockly.core.event.ArtifactUploadCompletedEvent;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.core.mapper.SessionMapper;
import com.mockly.core.service.LiveKitService;
import com.mockly.core.service.MinIOService;
import com.mockly.core.service.ReportService;
import com.mockly.data.entity.Artifact;
import com.mockly.data.entity.Session;
import com.mockly.data.entity.SessionParticipant;
import com.mockly.data.enums.ArtifactType;
import com.mockly.data.enums.SessionStatus;
import com.mockly.data.repository.ArtifactRepository;
import com.mockly.data.repository.SessionParticipantRepository;
import com.mockly.data.repository.SessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for handling LiveKit webhook events.
 * LiveKit sends webhooks for room and participant events.
 */
@RestController
@RequestMapping("/api/webhooks/livekit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "LiveKit webhook endpoints")
public class LiveKitWebhookController {

    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final ArtifactRepository artifactRepository;
    private final SessionEventPublisher eventPublisher;
    private final SessionMapper sessionMapper;
    private final ObjectMapper objectMapper;
    private final ReportService reportService;
    private final LiveKitService liveKitService;
    private final MinIOService minIOService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${livekit.webhook-secret:}")
    private String webhookSecret;

    @Value("${livekit.api-secret:}")
    private String liveKitApiSecret;

    @Value("${livekit.api-key:}")
    private String liveKitApiKey;

    @PostMapping(consumes = {"application/webhook+json", "application/json"})
    @Operation(
            summary = "Handle LiveKit webhook",
            description = "Receives and processes LiveKit webhook events (room/participant/egress lifecycle)."
    )
    @Transactional
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String rawBody) {

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawBody, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("Invalid LiveKit webhook payload: {}", rawBody, e);
            return ResponseEntity.badRequest().build();
        }

        if (isWebhookSignatureValidationEnabled()) {
            if (authorization == null || authorization.isBlank()) {
                log.warn("LiveKit webhook rejected: missing Authorization header");
                return ResponseEntity.status(401).build();
            }

            if (!verifyWebhookSignature(authorization, rawBody)) {
                log.warn("LiveKit webhook rejected: invalid signature");
                return ResponseEntity.status(401).build();
            }
        }

        String event = stringValue(payload.get("event"));
        if (event == null) {
            log.warn("LiveKit webhook missing event field");
            return ResponseEntity.badRequest().build();
        }

        try {
            switch (event) {
                case "room_started" -> handleRoomStarted(payload);
                case "room_finished" -> handleRoomFinished(payload);
                case "participant_joined" -> handleParticipantJoined(payload);
                case "participant_left" -> handleParticipantLeft(payload);
                case "egress_started" -> handleEgressStarted(payload);
                case "egress_ended" -> handleEgressEnded(payload);
                case "egress_updated" -> log.debug("Received egress_updated webhook event");
                default -> log.info("Unhandled LiveKit webhook event: {}", event);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing LiveKit webhook event: {}", event, e);
            return ResponseEntity.status(500).build();
        }
    }

    private void handleRoomStarted(Map<String, Object> payload) {
        UUID sessionId = extractSessionIdFromPayload(payload);
        if (sessionId == null) {
            return;
        }

        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getStatus() == SessionStatus.SCHEDULED) {
                session.setStatus(SessionStatus.ACTIVE);
                session.setStartsAt(OffsetDateTime.now());
                Session updated = sessionRepository.save(session);
                log.info("Session {} started from LiveKit room_started event", sessionId);

                var sessionResponse = sessionMapper.toResponse(updated);
                eventPublisher.publishSessionUpdated(updated, sessionResponse);
            }
        });
    }

    private void handleRoomFinished(Map<String, Object> payload) {
        UUID sessionId = extractSessionIdFromPayload(payload);
        if (sessionId == null) {
            return;
        }

        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getStatus() != SessionStatus.ENDED) {
                session.setStatus(SessionStatus.ENDED);
                session.setEndsAt(OffsetDateTime.now());
                Session updated = sessionRepository.save(session);
                log.info("Session {} finished from LiveKit room_finished event", sessionId);

                var sessionResponse = sessionMapper.toResponse(updated);
                eventPublisher.publishSessionEnded(updated, sessionResponse);
                triggerReportIfPossible(updated);
            }
        });
    }

    private void handleParticipantJoined(Map<String, Object> payload) {
        UUID sessionId = extractSessionIdFromPayload(payload);
        UUID userId = extractParticipantUserId(payload);
        if (sessionId == null || userId == null) {
            return;
        }

        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("Participant joined event for unknown session: {}", sessionId);
            return;
        }

        if (session.getStatus() == SessionStatus.SCHEDULED) {
            session.setStatus(SessionStatus.ACTIVE);
            session.setStartsAt(OffsetDateTime.now());
            session = sessionRepository.save(session);
        }

        Optional<SessionParticipant> participantOpt = participantRepository.findBySessionIdAndUserId(sessionId, userId);
        if (participantOpt.isEmpty()) {
            log.warn("Participant joined event for non-member user {} in session {}", userId, sessionId);
            return;
        }

        SessionParticipant participant = participantOpt.get();
        participant.setJoinedAt(OffsetDateTime.now());
        participant.setLeftAt(null);
        participant = participantRepository.save(participant);

        Session refreshedSession = sessionRepository.findById(sessionId).orElse(session);
        maybeStartSessionRecording(refreshedSession);
        refreshedSession = sessionRepository.findById(sessionId).orElse(refreshedSession);
        var sessionResponse = sessionMapper.toResponse(refreshedSession);
        eventPublisher.publishParticipantJoined(refreshedSession, participant, sessionResponse);

        log.info("Participant {} joined session {} from LiveKit webhook", userId, sessionId);
    }

    private void handleParticipantLeft(Map<String, Object> payload) {
        UUID sessionId = extractSessionIdFromPayload(payload);
        UUID userId = extractParticipantUserId(payload);
        if (sessionId == null || userId == null) {
            return;
        }

        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("Participant left event for unknown session: {}", sessionId);
            return;
        }

        Optional<SessionParticipant> participantOpt = participantRepository.findBySessionIdAndUserId(sessionId, userId);
        if (participantOpt.isEmpty()) {
            log.warn("Participant left event for non-member user {} in session {}", userId, sessionId);
            return;
        }

        SessionParticipant participant = participantOpt.get();
        if (participant.getLeftAt() == null) {
            participant.setLeftAt(OffsetDateTime.now());
            participantRepository.save(participant);
        }

        Session refreshedSession = sessionRepository.findById(sessionId).orElse(session);
        long activeParticipants = participantRepository.findBySessionIdAndLeftAtIsNull(sessionId).size();

        if (activeParticipants == 0 && refreshedSession.getStatus() != SessionStatus.ENDED) {
            maybeStopSessionRecording(refreshedSession, "last participant left (webhook)");
            refreshedSession.setStatus(SessionStatus.ENDED);
            refreshedSession.setEndsAt(OffsetDateTime.now());
            refreshedSession = sessionRepository.save(refreshedSession);
            var sessionResponse = sessionMapper.toResponse(refreshedSession);
            eventPublisher.publishSessionEnded(refreshedSession, sessionResponse);
            triggerReportIfPossible(refreshedSession);
            log.info("Session {} marked ENDED after last participant left (webhook)", sessionId);
            return;
        }

        var sessionResponse = sessionMapper.toResponse(refreshedSession);
        eventPublisher.publishParticipantLeft(refreshedSession, userId, sessionResponse);
        log.info("Participant {} left session {} from LiveKit webhook", userId, sessionId);
    }

    private void handleEgressStarted(Map<String, Object> payload) {
        Map<String, Object> egressInfo = extractEgressInfo(payload);
        if (egressInfo == null) {
            log.warn("LiveKit egress_started missing egressInfo: {}", payload);
            return;
        }

        String roomName = extractString(egressInfo, "room_name", "roomName");
        String egressId = extractString(egressInfo, "egress_id", "egressId");
        UUID sessionId = extractSessionIdFromRoomName(roomName);
        if (sessionId == null || egressId == null || egressId.isBlank()) {
            log.warn("Cannot map egress_started to session. roomName={}, egressId={}", roomName, egressId);
            return;
        }

        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getRecordingId() == null || session.getRecordingId().isBlank()) {
                session.setRecordingId(egressId);
                sessionRepository.save(session);
            }
            log.info("Egress started for session {}: {}", sessionId, egressId);
        });
    }

    private void handleEgressEnded(Map<String, Object> payload) {
        Map<String, Object> egressInfo = extractEgressInfo(payload);
        if (egressInfo == null) {
            log.warn("LiveKit egress_ended missing egressInfo: {}", payload);
            return;
        }

        String roomName = extractString(egressInfo, "room_name", "roomName");
        String egressId = extractString(egressInfo, "egress_id", "egressId");
        UUID sessionId = extractSessionIdFromRoomName(roomName);
        if (sessionId == null) {
            log.warn("Cannot map egress_ended to session. roomName={}, egressId={}", roomName, egressId);
            return;
        }

        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("Egress ended for unknown session {} (egress={})", sessionId, egressId);
            return;
        }

        Map<String, Object> fileResult = extractFirstFileResult(egressInfo);
        if (fileResult == null) {
            log.warn("No file_results in egress_ended for session {} (egress={})", sessionId, egressId);
            clearRecordingIfMatches(session, egressId);
            return;
        }

        String location = extractString(fileResult, "location", "filename");
        if (location == null || location.isBlank()) {
            log.warn("Egress file result has empty location for session {} (egress={})", sessionId, egressId);
            clearRecordingIfMatches(session, egressId);
            return;
        }

        String objectName = minIOService.normalizeObjectName(location);
        try {
            if (!minIOService.objectExists(objectName)) {
                log.warn("Egress output object not found in storage yet for session {}: {}", sessionId, objectName);
                clearRecordingIfMatches(session, egressId);
                return;
            }
        } catch (Exception e) {
            log.warn("Could not verify egress output object for session {}: {}", sessionId, objectName, e);
        }

        Long sizeBytes = longValue(extractAny(fileResult, "size"));
        Long durationNanos = longValue(extractAny(fileResult, "duration"));
        Integer durationSec = toDurationSeconds(durationNanos);

        try {
            int insertedRows = artifactRepository.insertEgressArtifactIfAbsent(
                    sessionId,
                    ArtifactType.AUDIO_MIXED.name(),
                    objectName,
                    sizeBytes,
                    durationSec
            );
            if (insertedRows == 0) {
                log.info("Artifact already registered for session {} and object {}", sessionId, objectName);
                return;
            }

            Artifact artifact = artifactRepository.findBySessionIdAndStorageUrl(sessionId, objectName)
                    .orElseThrow(() -> new IllegalStateException(
                            "Inserted artifact is missing for session " + sessionId + " and object " + objectName
                    ));

            applicationEventPublisher.publishEvent(new ArtifactUploadCompletedEvent(
                    sessionId,
                    artifact.getId(),
                    artifact.getType(),
                    session.getCreatedBy()
            ));

            log.info("Registered egress artifact and published upload-complete event: session={}, artifact={}, egress={}, object={}",
                    sessionId, artifact.getId(), egressId, objectName);
        } catch (Exception e) {
            log.error("Failed to register egress artifact for session {} (egress={})", sessionId, egressId, e);
        } finally {
            clearRecordingIfMatches(session, egressId);
        }
    }

    private void triggerReportIfPossible(Session session) {
        try {
            reportService.triggerReportGeneration(session.getId(), session.getCreatedBy());
            log.info("Auto-triggered report generation after LiveKit session end: {}", session.getId());
        } catch (BadRequestException | ResourceNotFoundException e) {
            log.info("Skipping auto report trigger after LiveKit end for session {}: {}",
                    session.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to auto-trigger report for session {} after LiveKit end",
                    session.getId(), e);
        }
    }

    private void maybeStartSessionRecording(Session session) {
        if (!liveKitService.isEgressAutoRecordEnabled()) {
            return;
        }
        if (session.getRecordingId() != null && !session.getRecordingId().isBlank()) {
            return;
        }

        long activeParticipants = participantRepository.findBySessionIdAndLeftAtIsNull(session.getId()).size();
        if (activeParticipants < 2) {
            return;
        }

        try {
            String egressId = liveKitService.startRoomAudioRecording(session.getId());
            session.setRecordingId(egressId);
            sessionRepository.save(session);
            log.info("Auto-started recording from webhook flow for session {}: {}", session.getId(), egressId);
        } catch (Exception e) {
            log.error("Failed to auto-start recording from webhook flow for session {}", session.getId(), e);
        }
    }

    private void maybeStopSessionRecording(Session session, String reason) {
        if (!liveKitService.isEgressAutoRecordEnabled()) {
            return;
        }
        if (session.getRecordingId() == null || session.getRecordingId().isBlank()) {
            return;
        }

        try {
            liveKitService.stopEgress(session.getRecordingId());
            log.info("Requested recording stop from webhook flow for session {} (reason: {}), egress={}",
                    session.getId(), reason, session.getRecordingId());
        } catch (Exception e) {
            log.error("Failed to stop recording from webhook flow for session {} (reason: {})",
                    session.getId(), reason, e);
        }
    }

    private void clearRecordingIfMatches(Session session, String egressId) {
        if (session == null || egressId == null || egressId.isBlank()) {
            return;
        }
        if (egressId.equals(session.getRecordingId())) {
            session.setRecordingId(null);
            sessionRepository.save(session);
        }
    }

    private UUID extractSessionIdFromPayload(Map<String, Object> payload) {
        String roomName = extractRoomName(payload);
        if (roomName == null) {
            log.warn("LiveKit webhook payload does not contain room name: {}", payload);
            return null;
        }
        return extractSessionIdFromRoomName(roomName);
    }

    private UUID extractSessionIdFromRoomName(String roomName) {
        if (!roomName.startsWith("session-")) {
            log.warn("Ignoring LiveKit webhook for non-session room: {}", roomName);
            return null;
        }

        try {
            return UUID.fromString(roomName.substring("session-".length()));
        } catch (Exception e) {
            log.warn("Failed to extract session UUID from room name: {}", roomName, e);
            return null;
        }
    }

    private Map<String, Object> extractEgressInfo(Map<String, Object> payload) {
        Object info = extractAny(payload, "egressInfo", "egress_info");
        if (info instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) map;
            return casted;
        }
        return null;
    }

    private Map<String, Object> extractFirstFileResult(Map<String, Object> egressInfo) {
        Object value = extractAny(egressInfo, "file_results", "fileResults");
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (first instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) map;
            return casted;
        }
        return null;
    }

    private Object extractAny(Map<String, Object> source, String... keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (source.containsKey(key)) {
                return source.get(key);
            }
        }
        return null;
    }

    private String extractString(Map<String, Object> source, String... keys) {
        Object value = extractAny(source, keys);
        return stringValue(value);
    }

    private UUID extractParticipantUserId(Map<String, Object> payload) {
        Object participantObj = payload.get("participant");
        if (!(participantObj instanceof Map<?, ?> participantMap)) {
            log.warn("LiveKit participant payload missing: {}", payload);
            return null;
        }

        String identity = stringValue(participantMap.get("identity"));
        if (identity == null) {
            log.warn("LiveKit participant identity missing: {}", participantMap);
            return null;
        }
        if (identity.startsWith("EG_")) {
            log.debug("Ignoring egress participant identity in participant webhook: {}", identity);
            return null;
        }

        try {
            return UUID.fromString(identity);
        } catch (Exception e) {
            log.debug("Ignoring non-UUID participant identity in webhook: {}", identity);
            return null;
        }
    }

    private String extractRoomName(Map<String, Object> payload) {
        Object roomObj = payload.get("room");
        if (roomObj instanceof String roomName) {
            return roomName;
        }
        if (roomObj instanceof Map<?, ?> roomMap) {
            return stringValue(roomMap.get("name"));
        }
        return null;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer toDurationSeconds(Long durationNanos) {
        if (durationNanos == null || durationNanos < 0) {
            return null;
        }
        long sec = durationNanos / 1_000_000_000L;
        return sec > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sec;
    }

    private boolean isWebhookSignatureValidationEnabled() {
        return !resolvedWebhookSecret().isBlank();
    }

    private String resolvedWebhookSecret() {
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            return webhookSecret;
        }
        if (liveKitApiSecret != null && !liveKitApiSecret.isBlank()) {
            return liveKitApiSecret;
        }
        return "";
    }

    private boolean verifyWebhookSignature(String authorization, String rawBody) {
        try {
            String token = extractJwtFromAuthorizationHeader(authorization);
            if (token == null) {
                return false;
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String signingInput = parts[0] + "." + parts[1];
            byte[] expectedSignature = hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8), resolvedWebhookSecret());
            if (!matchesBase64EncodedBytes(parts[2], expectedSignature)) {
                return false;
            }

            String claimsJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = objectMapper.readValue(claimsJson, new TypeReference<>() {
            });

            if (liveKitApiKey != null && !liveKitApiKey.isBlank()) {
                String iss = stringValue(claims.get("iss"));
                if (!liveKitApiKey.equals(iss)) {
                    return false;
                }
            }

            long now = System.currentTimeMillis() / 1000;
            Long notBefore = longClaim(claims.get("nbf"));
            Long expiresAt = longClaim(claims.get("exp"));

            if (notBefore != null && now + 30 < notBefore) {
                return false;
            }
            if (expiresAt != null && now - 30 > expiresAt) {
                return false;
            }

            String sha256Claim = stringValue(claims.get("sha256"));
            if (sha256Claim == null || sha256Claim.isBlank()) {
                return false;
            }

            byte[] expectedHash = MessageDigest.getInstance("SHA-256")
                    .digest(rawBody.getBytes(StandardCharsets.UTF_8));
            return matchesBase64EncodedBytes(sha256Claim, expectedHash);
        } catch (Exception e) {
            log.error("Error verifying LiveKit webhook signature", e);
            return false;
        }
    }

    private String extractJwtFromAuthorizationHeader(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        return authorization.trim();
    }

    private byte[] hmacSha256(byte[] data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        return mac.doFinal(data);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(withBase64Padding(value));
    }

    private String withBase64Padding(String value) {
        int padding = value.length() % 4;
        if (padding == 0) {
            return value;
        }
        return value + "=".repeat(4 - padding);
    }

    private Long longClaim(Object claimValue) {
        if (claimValue instanceof Number number) {
            return number.longValue();
        }
        if (claimValue instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean matchesBase64EncodedBytes(String encoded, byte[] expectedBytes) {
        return decodeBase64AndMatch(encoded, expectedBytes, Base64.getDecoder())
                || decodeBase64AndMatch(encoded, expectedBytes, Base64.getUrlDecoder());
    }

    private boolean decodeBase64AndMatch(String encoded, byte[] expectedBytes, Base64.Decoder decoder) {
        try {
            byte[] actualBytes = decoder.decode(withBase64Padding(encoded));
            return MessageDigest.isEqual(actualBytes, expectedBytes);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
