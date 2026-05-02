package com.mockly.core.dto.file;

import java.util.UUID;

public record AvatarUploadResponse(
        UUID fileId,
        String uploadUrl,
        String objectName,
        Integer expiresInSeconds
) {}
