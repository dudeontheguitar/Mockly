package com.mockly.core.dto.notification;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> items,
        long unreadCount
) {}
