package com.mockly.api.websocket;

import com.mockly.core.dto.chat.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishMessageCreated(UUID conversationId, MessageResponse message) {
        ChatMessageEvent event = new ChatMessageEvent("MESSAGE_CREATED", conversationId, message);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, event);
    }

    public void publishMessageRead(UUID conversationId, UUID userId, UUID lastReadMessageId) {
        ChatReadEvent event = new ChatReadEvent("MESSAGE_READ", conversationId, userId, lastReadMessageId);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, event);
    }

    public record ChatMessageEvent(
            String type,
            UUID conversationId,
            MessageResponse message
    ) {}

    public record ChatReadEvent(
            String type,
            UUID conversationId,
            UUID userId,
            UUID lastReadMessageId
    ) {}
}
