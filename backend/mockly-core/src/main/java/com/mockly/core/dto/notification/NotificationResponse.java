package com.mockly.core.dto.notification;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String message,
        Boolean isRead,
        OffsetDateTime createdAt,
        Map<String, Object> payload
) {}
