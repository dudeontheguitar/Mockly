package com.mockly.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockly.api.websocket.SessionEventPublisher;
import com.mockly.core.mapper.SessionMapper;
import com.mockly.data.entity.Session;
import com.mockly.data.entity.SessionParticipant;
import com.mockly.data.enums.SessionStatus;
import com.mockly.data.repository.SessionParticipantRepository;
import com.mockly.data.repository.SessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
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
    private final SessionEventPublisher eventPublisher;
    private final SessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    @Value("${livekit.webhook-secret:}")
    private String webhookSecret;

    @Value("${livekit.api-secret:}")
    private String liveKitApiSecret;

    @Value("${livekit.api-key:}")
    private String liveKitApiKey;

    @PostMapping(consumes = {"application/webhook+json", "application/json"})
    @Operation(
            summary = "Handle LiveKit webhook",
            description = "Receives and processes LiveKit webhook events (room_started, room_finished, participant_joined, participant_left)"
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
            refreshedSession.setStatus(SessionStatus.ENDED);
            refreshedSession.setEndsAt(OffsetDateTime.now());
            refreshedSession = sessionRepository.save(refreshedSession);
            var sessionResponse = sessionMapper.toResponse(refreshedSession);
            eventPublisher.publishSessionEnded(refreshedSession, sessionResponse);
            log.info("Session {} marked ENDED after last participant left (webhook)", sessionId);
            return;
        }

        var sessionResponse = sessionMapper.toResponse(refreshedSession);
        eventPublisher.publishParticipantLeft(refreshedSession, userId, sessionResponse);
        log.info("Participant {} left session {} from LiveKit webhook", userId, sessionId);
    }

    private UUID extractSessionIdFromPayload(Map<String, Object> payload) {
        String roomName = extractRoomName(payload);
        if (roomName == null) {
            log.warn("LiveKit webhook payload does not contain room name: {}", payload);
            return null;
        }

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

        try {
            return UUID.fromString(identity);
        } catch (Exception e) {
            log.warn("LiveKit participant identity is not UUID: {}", identity, e);
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
        return String.valueOf(value);
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
