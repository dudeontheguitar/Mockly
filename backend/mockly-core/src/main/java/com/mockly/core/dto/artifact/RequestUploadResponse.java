package com.mockly.core.dto.artifact;

import java.util.UUID;




public record RequestUploadResponse(
        UUID artifactId,
        String uploadUrl,
        String objectName,
        Integer expiresInSeconds
) {}

