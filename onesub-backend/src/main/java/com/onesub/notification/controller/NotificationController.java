package com.onesub.notification.controller;

import com.onesub.auth.UserPrincipal;
import com.onesub.common.PageResponse;
import com.onesub.notification.dto.NotificationPreferencesRequest;
import com.onesub.notification.dto.NotificationPreferencesResponse;
import com.onesub.notification.dto.NotificationResponse;
import com.onesub.notification.service.NotificationService;
import com.onesub.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "scheduledAt") Pageable pageable) {
        return ApiResponse.success(PageResponse.from(
                notificationService.list(principal.getUser(), pageable)));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.success("Notification marked read",
                notificationService.markRead(id, principal.getUser()));
    }

    @GetMapping("/preferences")
    public ApiResponse<NotificationPreferencesResponse> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                notificationService.getPreferences(principal.getUser()));
    }

    @PutMapping("/preferences")
    public ApiResponse<NotificationPreferencesResponse> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationPreferencesRequest request) {
        return ApiResponse.success("Notification preferences updated",
                notificationService.updatePreferences(principal.getUser(), request));
    }
}
