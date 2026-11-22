package com.onesub.notification.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferencesRequest(
        @NotNull Boolean reminder7Days,
        @NotNull Boolean reminder3Days,
        @NotNull Boolean reminder1Day,
        @NotNull Boolean emailEnabled
) {
}
