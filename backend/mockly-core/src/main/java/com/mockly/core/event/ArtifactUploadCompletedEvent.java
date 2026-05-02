package com.mockly.core.event;

import com.mockly.data.enums.ArtifactType;

import java.util.UUID;




public record ArtifactUploadCompletedEvent(
        UUID sessionId,
        UUID artifactId,
        ArtifactType artifactType,
        UUID uploadedBy
) {}
