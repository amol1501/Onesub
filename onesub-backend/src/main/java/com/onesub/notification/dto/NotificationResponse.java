package com.onesub.notification.dto;

import com.onesub.enums.NotificationStatus;
import com.onesub.notification.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long subscriptionId,
        String notificationType,
        Instant scheduledAt,
        Instant sentAt,
        NotificationStatus status,
        boolean read
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getSubscription() == null ? null : notification.getSubscription().getId(),
                notification.getNotificationType(),
                notification.getScheduledAt(),
                notification.getSentAt(),
                notification.getStatus(),
                notification.getReadAt() != null
        );
    }
}
