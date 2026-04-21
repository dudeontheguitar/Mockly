package com.mockly.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockly.core.dto.session.LiveKitTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
        videoGrant.put("roomAdmin", true);
        videoGrant.put("room", roomId);
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
