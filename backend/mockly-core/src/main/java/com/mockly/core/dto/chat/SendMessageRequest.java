package com.mockly.core.dto.chat;

import com.mockly.data.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Text is required")
        @Size(max = 5000, message = "Message must not exceed 5000 characters")
        String text,

        @NotNull(message = "Message type is required")
        MessageType type
) {}
