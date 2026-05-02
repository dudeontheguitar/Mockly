package com.mockly.core.dto.chat;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDirectConversationRequest(
        @NotNull(message = "User ID is required")
        UUID userId
) {}
