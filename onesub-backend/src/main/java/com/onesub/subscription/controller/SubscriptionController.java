package com.onesub.subscription.controller;

import com.onesub.auth.UserPrincipal;
import com.onesub.common.PageResponse;
import com.onesub.response.ApiResponse;
import com.onesub.subscription.dto.SubscriptionRequest;
import com.onesub.subscription.dto.SubscriptionResponse;
import com.onesub.subscription.service.SubscriptionService;
import com.onesub.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SubscriptionResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "nextBillingDate") Pageable pageable) {
        return ApiResponse.success(PageResponse.from(
                subscriptionService.list(principal.getUser(), pageable)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubscriptionResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubscriptionRequest request) {
        return ApiResponse.success("Subscription created",
                subscriptionService.create(principal.getUser(), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SubscriptionResponse> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.success(subscriptionService.get(id, principal.getUser()));
    }

    @PutMapping("/{id}")
    public ApiResponse<SubscriptionResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionRequest request) {
        return ApiResponse.success("Subscription updated",
                subscriptionService.update(id, principal.getUser(), request));
    }

    @PatchMapping("/{id}/pause")
    public ApiResponse<SubscriptionResponse> pause(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.success("Subscription paused",
                subscriptionService.pause(id, principal.getUser()));
    }

    @PatchMapping("/{id}/resume")
    public ApiResponse<SubscriptionResponse> resume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.success("Subscription resumed",
                subscriptionService.resume(id, principal.getUser()));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<SubscriptionResponse> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.success("Subscription tracking cancelled",
                subscriptionService.cancel(id, principal.getUser()));
    }
}
