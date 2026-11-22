package com.onesub.report.controller;

import com.onesub.auth.UserPrincipal;
import com.onesub.report.dto.MerchantWiseReport;
import com.onesub.report.dto.SpendingReport;
import com.onesub.report.service.ReportService;
import com.onesub.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly")
    public ApiResponse<SpendingReport> monthly(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(reportService.monthly(principal.getUser()));
    }

    @GetMapping("/yearly")
    public ApiResponse<SpendingReport> yearly(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(reportService.yearly(principal.getUser()));
    }

    @GetMapping("/merchant-wise")
    public ApiResponse<MerchantWiseReport> merchantWise(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(reportService.merchantWise(principal.getUser()));
    }
}
