package com.mockly.core.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateInterviewSlotRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must not exceed 150 characters")
        String title,

        @NotBlank(message = "Company is required")
        @Size(max = 150, message = "Company must not exceed 150 characters")
        String company,

        @Size(max = 150, message = "Location must not exceed 150 characters")
        String location,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        @NotNull(message = "Scheduled time is required")
        OffsetDateTime scheduledAt,

        @NotNull(message = "Duration is required")
        @Positive(message = "Duration must be positive")
        Integer durationMinutes
) {}
