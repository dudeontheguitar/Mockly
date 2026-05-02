package com.mockly.core.service;

import com.mockly.core.dto.notification.NotificationListResponse;
import com.mockly.core.dto.notification.NotificationResponse;
import com.mockly.core.dto.notification.ReadAllNotificationsResponse;
import com.mockly.core.exception.ForbiddenException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.data.entity.Notification;
import com.mockly.data.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse createNotification(UUID userId, String type, String title, String message, Map<String, Object> payload) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .payload(payload == null ? Map.of() : payload)
                .isRead(false)
                .build();

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public NotificationListResponse listNotifications(UUID userId, int page, int size, boolean unreadOnly) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<NotificationResponse> items = (unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalse(userId, pageable)
                : notificationRepository.findByUserId(userId, pageable))
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new NotificationListResponse(items, notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this notification");
        }
        notification.setIsRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public ReadAllNotificationsResponse markAllRead(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(
                userId,
                PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        notifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(notifications);
        return new ReadAllNotificationsResponse(0);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                notification.getPayload()
        );
    }
}
