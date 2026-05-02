package com.mockly.core.dto.chat;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationLastMessageResponse(
        UUID id,
        String text,
        UUID senderId,
        String senderDisplayName,
        OffsetDateTime createdAt
) {}
