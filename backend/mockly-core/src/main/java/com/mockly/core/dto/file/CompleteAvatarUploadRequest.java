package com.mockly.core.dto.file;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CompleteAvatarUploadRequest(
        @NotNull(message = "File ID is required")
        UUID fileId
) {}
