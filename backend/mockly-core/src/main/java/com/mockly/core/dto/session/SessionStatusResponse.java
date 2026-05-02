package com.mockly.core.dto.session;

import com.mockly.data.enums.SessionStatus;

import java.util.UUID;

public record SessionStatusResponse(
        UUID id,
        SessionStatus status
) {}
