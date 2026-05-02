package com.mockly.api.controller;

import com.mockly.api.websocket.ChatEventPublisher;
import com.mockly.core.dto.chat.*;
import com.mockly.core.service.ChatService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Conversation and message endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;
    private final ChatEventPublisher chatEventPublisher;

    @GetMapping("/conversations")
    public ResponseEntity<ConversationListResponse> listConversations(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(chatService.listConversations(userId));
    }

    @PostMapping("/conversations/direct")
    public ResponseEntity<ConversationResponse> createDirectConversation(
            Authentication authentication,
            @Valid @RequestBody CreateDirectConversationRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(chatService.createOrGetDirectConversation(userId, request.userId()));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            Authentication authentication,
            @PathVariable UUID conversationId) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(chatService.getConversation(userId, conversationId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageListResponse> listMessages(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(chatService.listMessages(userId, conversationId, page, size));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        MessageResponse response = chatService.sendMessage(userId, conversationId, request);
        chatEventPublisher.publishMessageCreated(conversationId, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ReadConversationResponse> markRead(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody ReadConversationRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        ReadConversationResponse response = chatService.markRead(userId, conversationId, request.lastReadMessageId());
        chatEventPublisher.publishMessageRead(conversationId, userId, request.lastReadMessageId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<DeleteMessageResponse> deleteMessage(
            Authentication authentication,
            @PathVariable UUID messageId) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(chatService.deleteOwnMessage(userId, messageId));
    }
}
