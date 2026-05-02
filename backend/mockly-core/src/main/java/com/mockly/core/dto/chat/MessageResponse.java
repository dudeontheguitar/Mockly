package com.mockly.core.dto.chat;

import com.mockly.data.enums.MessageStatus;
import com.mockly.data.enums.MessageType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderDisplayName,
        String senderAvatarUrl,
        String text,
        MessageType type,
        MessageStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
