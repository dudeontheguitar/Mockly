package com.mockly.core.dto.chat;

import java.util.UUID;

public record ReadConversationResponse(
        UUID conversationId,
        long unreadCount
) {}
