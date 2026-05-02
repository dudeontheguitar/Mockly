package com.mockly.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockly.core.dto.session.LiveKitTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for LiveKit WebRTC integration.
 * Handles token generation and room management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveKitService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${livekit.url:ws://localhost:7880}")
    private String liveKitUrl;

    @Value("${livekit.api-url:}")
    private String liveKitApiUrl;

    @Value("${livekit.api-key:}")
    private String apiKey;

    @Value("${livekit.api-secret:}")
    private String apiSecret;

    @Value("${livekit.egress.auto-record:true}")
    private boolean egressAutoRecordEnabled;

    @Value("${livekit.egress.s3-endpoint:${minio.endpoint:http://localhost:19000}}")
    private String egressS3Endpoint;

    @Value("${livekit.egress.s3-region:}")
    private String egressS3Region;

    @Value("${livekit.egress.s3-force-path-style:true}")
    private boolean egressS3ForcePathStyle;

    @Value("${livekit.egress.file-type:MP3}")
    private String egressFileType;

    @Value("${livekit.egress.webhook-url:}")
    private String egressWebhookUrl;

    @Value("${livekit.egress.webhook-signing-key:${livekit.api-key:}}")
    private String egressWebhookSigningKey;

    @Value("${minio.access-key:minioadmin}")
    private String minioAccessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String minioSecretKey;

    @Value("${minio.bucket-name:mockly-artifacts}")
    private String minioBucketName;

    private static final String ROOM_PREFIX = "session-";
    private static final int TOKEN_EXPIRATION_HOURS = 6;
    private static final int SERVICE_TOKEN_EXPIRATION_SECONDS = 300;
    private static final int DEFAULT_EMPTY_ROOM_TIMEOUT_SECONDS = 600;

    /**
     * Generate a LiveKit access token for a user to join a session room.
     *
     * @param sessionId Session ID (used as room ID)
     * @param userId User ID (used as identity)
     * @param displayName User display name
     * @return LiveKitTokenResponse with token, roomId, and URL
     */
    public LiveKitTokenResponse generateToken(UUID sessionId, UUID userId, String displayName) {
        log.info("Generating LiveKit token for session: {}, user: {}", sessionId, userId);

        validateLiveKitCredentialsConfigured();

        String roomId = ROOM_PREFIX + sessionId.toString();
        String identity = userId.toString();

        try {
            // Generate JWT token manually for LiveKit
            // Format: header.payload.signature
            long now = System.currentTimeMillis() / 1000;
            long exp = now + (TOKEN_EXPIRATION_HOURS * 3600);

            // Build claims with name/metadata
            String nameClaim = displayName != null && !displayName.isBlank() 
                    ? String.format(",\"name\":\"%s\"", escapeJson(displayName))
                    : "";
            String claimsJson = String.format(
                "{\"iss\":\"%s\",\"sub\":\"%s\",\"nbf\":%d,\"exp\":%d,\"video\":{\"room\":\"%s\",\"roomJoin\":true,\"canPublish\":true,\"canSubscribe\":true}%s}",
                apiKey, identity, now, exp, roomId, nameClaim
            );

            // Create JWT header
            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(claimsJson.getBytes(StandardCharsets.UTF_8));

            // Create signature
            String data = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signatureBytes);

            String tokenString = data + "." + signature;

            log.info("LiveKit token generated successfully for room: {}", roomId);

            return new LiveKitTokenResponse(tokenString, roomId, liveKitUrl);
        } catch (Exception e) {
            log.error("Failed to generate LiveKit token", e);
            throw new RuntimeException("Failed to generate LiveKit token: " + e.getMessage(), e);
        }
    }

    /**
     * Create a LiveKit room ID for a session.
     * In LiveKit, rooms are created automatically when the first participant joins.
     *
     * @param sessionId Session ID
     * @return Room ID
     */
    public String createRoom(UUID sessionId) {
        String roomId = ROOM_PREFIX + sessionId.toString();

        try {
            createRoomInLiveKit(roomId);
            log.info("LiveKit room created successfully: {}", roomId);
        } catch (Exception e) {
            log.error("Failed to create LiveKit room: {}", roomId, e);
            throw new RuntimeException("Failed to create LiveKit room: " + e.getMessage(), e);
        }

        return roomId;
    }

    public boolean isEgressAutoRecordEnabled() {
        return egressAutoRecordEnabled;
    }

    public String startRoomAudioRecording(UUID sessionId) {
        validateLiveKitCredentialsConfigured();

        String roomId = ROOM_PREFIX + sessionId;
        String token = generateEgressServiceToken(roomId);

        Map<String, Object> fileOutput = new HashMap<>();
        fileOutput.put("file_type", normalizedEgressFileType());
        fileOutput.put("filepath", buildEgressFilePath(sessionId));

        Map<String, Object> s3 = new HashMap<>();
        s3.put("access_key", minioAccessKey);
        s3.put("secret", minioSecretKey);
        s3.put("bucket", minioBucketName);
        s3.put("endpoint", egressS3Endpoint);
        s3.put("force_path_style", egressS3ForcePathStyle);
        if (egressS3Region != null && !egressS3Region.isBlank()) {
            s3.put("region", egressS3Region);
        }
        fileOutput.put("s3", s3);

        Map<String, Object> body = new HashMap<>();
        body.put("room_name", roomId);
        body.put("audio_only", true);
        body.put("file_outputs", List.of(fileOutput));

        if (egressWebhookUrl != null && !egressWebhookUrl.isBlank()
                && egressWebhookSigningKey != null && !egressWebhookSigningKey.isBlank()) {
            body.put("webhooks", List.of(Map.of(
                    "url", egressWebhookUrl,
                    "signing_key", egressWebhookSigningKey
            )));
        }

        try {
            Map<String, Object> response = liveKitClient().post()
                    .uri("/twirp/livekit.Egress/StartRoomCompositeEgress")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            String egressId = extractString(response, "egress_id", "egressId");
            if (egressId == null || egressId.isBlank()) {
                throw new IllegalStateException("LiveKit egress started but no egress_id was returned");
            }

            log.info("Started LiveKit room audio recording: session={}, room={}, egress={}",
                    sessionId, roomId, egressId);
            return egressId;
        } catch (WebClientResponseException e) {
            String details = e.getResponseBodyAsString();
            throw new RuntimeException("Failed to start LiveKit egress: status=" + e.getStatusCode()
                    + ", details=" + details, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start LiveKit egress", e);
        }
    }

    public void stopEgress(String egressId) {
        if (egressId == null || egressId.isBlank()) {
            return;
        }

        validateLiveKitCredentialsConfigured();
        String token = generateEgressServiceToken(null);

        Map<String, Object> body = Map.of("egress_id", egressId);

        try {
            liveKitClient().post()
                    .uri("/twirp/livekit.Egress/StopEgress")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Stopped LiveKit egress: {}", egressId);
        } catch (WebClientResponseException.NotFound e) {
            log.info("LiveKit egress {} already stopped or not found", egressId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop LiveKit egress " + egressId, e);
        }
    }

    /**
     * Delete a LiveKit room (cleanup after session ends).
     * Note: This requires LiveKit RoomService API calls.
     * For now, rooms are automatically cleaned up by LiveKit after being empty.
     *
     * @param roomId Room ID to delete
     */
    public void deleteRoom(String roomId) {
        if (!isLiveKitConfigured()) {
            log.warn("LiveKit API key/secret not configured, skipping room deletion for {}", roomId);
            return;
        }

        try {
            deleteRoomInLiveKit(roomId);
            log.info("LiveKit room deleted successfully: {}", roomId);
        } catch (WebClientResponseException.NotFound e) {
            log.info("LiveKit room {} not found during deletion, treating as already cleaned up", roomId);
        } catch (Exception e) {
            // Best effort cleanup: don't fail session end because cleanup call failed
            log.error("Failed to delete LiveKit room {}", roomId, e);
        }
    }

    private void createRoomInLiveKit(String roomId) {
        String token = generateRoomServiceTokenForCreate(roomId);
        Map<String, Object> body = new HashMap<>();
        body.put("name", roomId);
        body.put("empty_timeout", DEFAULT_EMPTY_ROOM_TIMEOUT_SECONDS);

        liveKitClient().post()
                .uri("/twirp/livekit.RoomService/CreateRoom")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private void deleteRoomInLiveKit(String roomId) {
        String token = generateRoomServiceTokenForDelete(roomId);
        Map<String, Object> body = Map.of("room", roomId);

        liveKitClient().post()
                .uri("/twirp/livekit.RoomService/DeleteRoom")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private WebClient liveKitClient() {
        return webClientBuilder
                .baseUrl(resolveLiveKitHttpBaseUrl())
                .build();
    }

    private String resolveLiveKitHttpBaseUrl() {
        String rawUrl = (liveKitApiUrl != null && !liveKitApiUrl.isBlank()) ? liveKitApiUrl : liveKitUrl;
        if (!rawUrl.startsWith("http://")
                && !rawUrl.startsWith("https://")
                && !rawUrl.startsWith("ws://")
                && !rawUrl.startsWith("wss://")) {
            rawUrl = "http://" + rawUrl;
        }

        try {
            URI uri = new URI(rawUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();

            if (host == null || scheme == null) {
                throw new IllegalStateException("Invalid LiveKit URL: " + liveKitUrl);
            }

            String normalizedScheme = switch (scheme.toLowerCase()) {
                case "ws" -> "http";
                case "wss" -> "https";
                default -> scheme.toLowerCase();
            };

            StringBuilder baseUrl = new StringBuilder(normalizedScheme)
                    .append("://")
                    .append(host);

            if (port != -1) {
                baseUrl.append(":").append(port);
            }

            return baseUrl.toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid livekit.api-url/livekit.url: " + rawUrl, e);
        }
    }

    private String generateRoomServiceTokenForCreate(String roomId) {
        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("roomCreate", true);
        videoGrant.put("roomList", true);
        videoGrant.put("roomAdmin", true);
        videoGrant.put("room", roomId);
        return generateServiceToken(videoGrant);
    }

    private String generateRoomServiceTokenForDelete(String roomId) {
        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("roomCreate", true);
        videoGrant.put("roomAdmin", true);
        videoGrant.put("room", roomId);
        return generateServiceToken(videoGrant);
    }

    private String generateEgressServiceToken(String roomId) {
        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("roomRecord", true);
        videoGrant.put("roomAdmin", true);
        if (roomId != null && !roomId.isBlank()) {
            videoGrant.put("room", roomId);
        }
        return generateServiceToken(videoGrant);
    }

    private String generateServiceToken(Map<String, Object> videoGrant) {
        validateLiveKitCredentialsConfigured();

        long now = System.currentTimeMillis() / 1000;
        long exp = now + SERVICE_TOKEN_EXPIRATION_SECONDS;

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", apiKey);
        claims.put("sub", "mockly-backend");
        claims.put("nbf", now);
        claims.put("exp", exp);
        claims.put("video", videoGrant);

        return buildSignedToken(claims);
    }

    private String buildSignedToken(Map<String, Object> claims) {
        try {
            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payloadJson = objectMapper.writeValueAsString(claims);

            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String data = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            return data + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signed token", e);
        }
    }

    private void validateLiveKitCredentialsConfigured() {
        if (!isLiveKitConfigured()) {
            throw new IllegalStateException("LiveKit API key and secret must be configured");
        }
    }

    private boolean isLiveKitConfigured() {
        return apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank();
    }

    private String normalizedEgressFileType() {
        String normalized = egressFileType == null ? "" : egressFileType.trim().toUpperCase();
        return switch (normalized) {
            case "MP3", "OGG", "MP4" -> normalized;
            default -> "MP3";
        };
    }

    private String buildEgressFilePath(UUID sessionId) {
        String extension = switch (normalizedEgressFileType()) {
            case "OGG" -> "ogg";
            case "MP4" -> "mp4";
            default -> "mp3";
        };
        return "sessions/" + sessionId + "/recordings/{time}." + extension;
    }

    private String extractString(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                String str = String.valueOf(value);
                if (!str.isBlank()) {
                    return str;
                }
            }
        }
        return null;
    }

    /**
     * Escape JSON string to prevent injection.
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
