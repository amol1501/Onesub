package com.onesub.notification.dto;

import com.onesub.notification.entity.NotificationPreferences;

public record NotificationPreferencesResponse(
        boolean reminder7Days,
        boolean reminder3Days,
        boolean reminder1Day,
        boolean emailEnabled
) {
    public static NotificationPreferencesResponse from(NotificationPreferences preferences) {
        return new NotificationPreferencesResponse(
                preferences.isReminder7Days(),
                preferences.isReminder3Days(),
                preferences.isReminder1Day(),
                preferences.isEmailEnabled()
        );
    }
}
