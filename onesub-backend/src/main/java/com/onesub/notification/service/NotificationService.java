package com.onesub.notification.service;

import com.onesub.enums.NotificationStatus;
import com.onesub.exception.ResourceNotFoundException;
import com.onesub.notification.dto.NotificationPreferencesRequest;
import com.onesub.notification.dto.NotificationPreferencesResponse;
import com.onesub.notification.dto.NotificationResponse;
import com.onesub.notification.entity.Notification;
import com.onesub.notification.entity.NotificationPreferences;
import com.onesub.notification.repository.NotificationPreferencesRepository;
import com.onesub.notification.repository.NotificationRepository;
import com.onesub.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository preferencesRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationPreferencesRepository preferencesRepository) {
        this.notificationRepository = notificationRepository;
        this.preferencesRepository = preferencesRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(User user, Pageable pageable) {
        return notificationRepository.findByUser(user, pageable).map(NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markRead(Long id, User user) {
        Notification notification = notificationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setReadAt(Instant.now());
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getPreferences(User user) {
        return NotificationPreferencesResponse.from(preferencesRepository.findByUserId(user.getId())
                .orElseGet(() -> defaultPreferences(user)));
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(
            User user, NotificationPreferencesRequest request) {
        NotificationPreferences preferences = preferencesRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    NotificationPreferences created = new NotificationPreferences();
                    created.setUserId(user.getId());
                    return created;
                });
        preferences.setReminder7Days(request.reminder7Days());
        preferences.setReminder3Days(request.reminder3Days());
        preferences.setReminder1Day(request.reminder1Day());
        preferences.setEmailEnabled(request.emailEnabled());
        return NotificationPreferencesResponse.from(preferencesRepository.save(preferences));
    }

    private NotificationPreferences defaultPreferences(User user) {
        NotificationPreferences preferences = new NotificationPreferences();
        preferences.setUserId(user.getId());
        return preferences;
    }
}
