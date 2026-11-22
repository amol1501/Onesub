package com.onesub.notification.repository;

import com.onesub.notification.entity.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPreferencesRepository
        extends JpaRepository<NotificationPreferences, Long> {
    Optional<NotificationPreferences> findByUserId(Long userId);
}
