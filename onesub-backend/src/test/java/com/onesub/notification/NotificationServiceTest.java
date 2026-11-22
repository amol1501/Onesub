package com.onesub.notification;

import com.onesub.exception.ResourceNotFoundException;
import com.onesub.notification.dto.NotificationPreferencesRequest;
import com.onesub.notification.repository.NotificationPreferencesRepository;
import com.onesub.notification.repository.NotificationRepository;
import com.onesub.notification.service.NotificationService;
import com.onesub.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationServiceTest {
    @Test
    void markReadRejectsNotificationOutsideUserOwnership() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        NotificationPreferencesRepository preferences = mock(NotificationPreferencesRepository.class);
        User user = new User();
        user.setId(8L);
        when(notifications.findByIdAndUser(123L, user)).thenReturn(Optional.empty());

        NotificationService service = new NotificationService(notifications, preferences);

        assertThatThrownBy(() -> service.markRead(123L, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void defaultPreferencesKeepAllRemindersEnabled() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        NotificationPreferencesRepository preferences = mock(NotificationPreferencesRepository.class);
        User user = new User();
        when(preferences.findByUserId(user.getId())).thenReturn(Optional.empty());

        NotificationService service = new NotificationService(notifications, preferences);
        var response = service.getPreferences(user);

        assertThat(response.reminder7Days()).isTrue();
        assertThat(response.reminder3Days()).isTrue();
        assertThat(response.reminder1Day()).isTrue();
        assertThat(response.emailEnabled()).isTrue();
    }
}
