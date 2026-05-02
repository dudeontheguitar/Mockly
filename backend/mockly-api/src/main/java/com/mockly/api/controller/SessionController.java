package com.mockly.api.controller;

import com.mockly.api.websocket.SessionEventPublisher;
import com.mockly.core.dto.session.CreateSessionRequest;
import com.mockly.core.dto.session.LiveKitTokenResponse;
import com.mockly.core.dto.session.SessionListResponse;
import com.mockly.core.dto.session.SessionResponse;
import com.mockly.core.dto.session.SessionStatusResponse;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.service.LiveKitService;
import com.mockly.core.service.SessionService;
import com.mockly.data.enums.SessionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions", description = "Interview session management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SessionController {

    private final SessionService sessionService;
    private final LiveKitService liveKitService;
    private final SessionEventPublisher eventPublisher;

    @PostMapping
    @Operation(
            summary = "Create a new session",
            description = "Creates a new interview session. User can have only one active session at a time."
    )
    public ResponseEntity<SessionResponse> createSession(
            Authentication authentication,
            @Valid @RequestBody CreateSessionRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        SessionResponse response = sessionService.createSession(userId, request);
        eventPublisher.publishSessionCreated(response.id(), response.createdBy(), response);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/join")
    @Operation(
            summary = "Join a session",
            description = "Authorize the current user to join an existing LiveKit session."
    )
    public ResponseEntity<SessionResponse> joinSession(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        SessionResponse response = sessionService.joinSession(id, userId);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/leave")
    @Operation(
            summary = "Leave a session",
            description = "Leave an active session. Updates participant's left_at timestamp."
    )
    public ResponseEntity<SessionStatusResponse> leaveSession(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        sessionService.leaveSession(id, userId);
        SessionResponse response = sessionService.getSession(id, userId);
        eventPublisher.publishParticipantLeft(id, userId, response);
        
        return ResponseEntity.ok(new SessionStatusResponse(response.id(), response.status()));
    }

    @PostMapping("/{id}/end")
    @Operation(
            summary = "End a session",
            description = "End a session. Only session creator or participants can end the session."
    )
    public ResponseEntity<SessionStatusResponse> endSession(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        sessionService.endSession(id, userId);
        SessionResponse response = sessionService.getSession(id, userId);
        eventPublisher.publishSessionEnded(id, response);
        
        return ResponseEntity.ok(new SessionStatusResponse(response.id(), response.status()));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get session by ID",
            description = "Returns session details. User must be the creator or a participant."
    )
    public ResponseEntity<SessionResponse> getSession(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        SessionResponse response = sessionService.getSession(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "List sessions",
            description = "Returns paginated list of sessions where user is creator or participant. Can be filtered by status."
    )
    public ResponseEntity<SessionListResponse> listSessions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SessionStatus status) {
        UUID userId = UUID.fromString(authentication.getName());
        SessionListResponse response = sessionService.listSessions(userId, page, size, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/active")
    @Operation(
            summary = "Get active session",
            description = "Returns the current user's active session (SCHEDULED or ACTIVE) where the user is creator or participant, if any."
    )
    public ResponseEntity<SessionResponse> getActiveSession(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Optional<SessionResponse> response = sessionService.getActiveSession(userId);
        return response.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/token")
    @Operation(
            summary = "Get LiveKit token",
            description = "Generates a LiveKit access token for joining the WebRTC room."
    )
    public ResponseEntity<LiveKitTokenResponse> getLiveKitToken(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());

        // Verify user has access to session
        SessionResponse session = sessionService.getSession(id, userId);
        if (session.status() == SessionStatus.ENDED || session.status() == SessionStatus.CANCELED) {
            throw new BadRequestException("Cannot generate LiveKit token for ended or canceled session");
        }

        // Get user display name from profile
        String displayName = sessionService.getUserDisplayName(userId);
        
        LiveKitTokenResponse response = liveKitService.generateToken(id, userId, displayName);
        return ResponseEntity.ok(response);
    }
}
