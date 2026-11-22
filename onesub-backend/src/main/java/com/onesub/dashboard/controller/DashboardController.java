package com.onesub.dashboard.controller;

import com.onesub.auth.UserPrincipal;
import com.onesub.dashboard.dto.DashboardSummary;
import com.onesub.dashboard.service.DashboardService;
import com.onesub.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> summary(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(dashboardService.summary(principal.getUser()));
    }

    @GetMapping("/upcoming-renewals")
    public ApiResponse<?> upcomingRenewals(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(dashboardService.summary(principal.getUser()).upcomingRenewals());
    }
}
