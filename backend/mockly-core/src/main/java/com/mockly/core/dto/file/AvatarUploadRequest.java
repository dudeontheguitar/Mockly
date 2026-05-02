package com.mockly.core.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AvatarUploadRequest(
        @NotBlank(message = "File name is required")
        @Size(max = 255, message = "File name must not exceed 255 characters")
        String fileName,

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be positive")
        Long fileSizeBytes,

        @NotBlank(message = "Content type is required")
        @Size(max = 100, message = "Content type must not exceed 100 characters")
        String contentType
) {}
