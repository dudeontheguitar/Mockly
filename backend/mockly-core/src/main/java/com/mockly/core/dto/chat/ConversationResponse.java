package com.mockly.core.dto.chat;

import com.mockly.data.enums.ConversationType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        ConversationType type,
        String title,
        ConversationLastMessageResponse lastMessage,
        long unreadCount,
        OffsetDateTime updatedAt,
        List<ConversationParticipantResponse> participants,
        OffsetDateTime createdAt
) {}
