package com.mockly.core.dto.chat;

import java.util.UUID;

public record DeleteMessageResponse(
        UUID id,
        boolean deleted
) {}
