package com.mockly.core.event;

import com.mockly.data.enums.ArtifactType;

import java.util.UUID;

/**
 * Domain event published after artifact upload metadata is finalized.
 */
public record ArtifactUploadCompletedEvent(
        UUID sessionId,
        UUID artifactId,
        ArtifactType artifactType,
        UUID uploadedBy
) {}
