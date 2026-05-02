package com.mockly.api.controller;

import com.mockly.core.dto.notification.NotificationListResponse;
import com.mockly.core.dto.notification.NotificationResponse;
import com.mockly.core.dto.notification.ReadAllNotificationsResponse;
import com.mockly.core.service.NotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification endpoints")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationListResponse> listNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(notificationService.listNotifications(userId, page, size, unreadOnly));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            Authentication authentication,
            @PathVariable UUID notificationId) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(notificationService.markRead(userId, notificationId));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ReadAllNotificationsResponse> markAllRead(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(notificationService.markAllRead(userId));
    }
}
