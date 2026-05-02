package com.mockly.core.dto.ml;

import java.util.UUID;




public record MLProcessRequest(
        UUID sessionId,
        UUID artifactId,
        String artifactUrl,
        String artifactType
) {}

