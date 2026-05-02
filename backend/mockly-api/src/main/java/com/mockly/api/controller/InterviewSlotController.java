package com.mockly.api.controller;

import com.mockly.core.dto.interview.*;
import com.mockly.core.service.InterviewSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/interview-slots")
@RequiredArgsConstructor
@Tag(name = "Interview Slots", description = "Public interview slot booking endpoints")
@SecurityRequirement(name = "bearerAuth")
public class InterviewSlotController {

    private final InterviewSlotService interviewSlotService;

    @GetMapping
    @Operation(summary = "List open interview slots", description = "Returns open interview slots for candidates")
    public ResponseEntity<InterviewSlotListResponse> listOpenSlots(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(interviewSlotService.listOpenSlots(userId));
    }

    @PostMapping
    @Operation(summary = "Create interview slot", description = "Creates a slot for the current interviewer")
    public ResponseEntity<InterviewSlotResponse> createSlot(
            Authentication authentication,
            @Valid @RequestBody CreateInterviewSlotRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(interviewSlotService.createSlot(userId, request));
    }

    @GetMapping("/my")
    @Operation(summary = "List my interview slots", description = "Returns slots created by the current interviewer")
    public ResponseEntity<InterviewSlotListResponse> getMySlots(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(interviewSlotService.getMySlots(userId));
    }

    @GetMapping("/{slotId}")
    @Operation(summary = "Get interview slot", description = "Returns interview slot details")
    public ResponseEntity<InterviewSlotResponse> getSlot(
            Authentication authentication,
            @PathVariable UUID slotId) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(interviewSlotService.getSlot(userId, slotId));
    }

    @PostMapping("/{slotId}/book")
    @Operation(summary = "Book interview slot", description = "Books an open slot and creates a session")
    public ResponseEntity<BookInterviewSlotResponse> bookSlot(
            Authentication authentication,
            @PathVariable UUID slotId) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(interviewSlotService.bookSlot(userId, slotId));
    }

    @PostMapping("/{slotId}/cancel")
    @Operation(summary = "Cancel interview slot", description = "Cancels a slot or existing booking")
    public ResponseEntity<CancelInterviewSlotResponse> cancelSlot(
            Authentication authentication,
            @PathVariable UUID slotId) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(interviewSlotService.cancelSlot(userId, slotId));
    }
}
