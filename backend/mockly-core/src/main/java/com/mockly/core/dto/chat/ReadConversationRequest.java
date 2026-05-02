package com.mockly.core.dto.chat;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReadConversationRequest(
        @NotNull(message = "Last read message ID is required")
        UUID lastReadMessageId
) {}
