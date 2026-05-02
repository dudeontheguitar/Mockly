package com.mockly.core.dto.artifact;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;




public record CompleteUploadRequest(
        @NotNull(message = "File size is required")
        @PositiveOrZero(message = "File size must be non-negative")
        Long fileSizeBytes,

        Integer durationSec
) {}

