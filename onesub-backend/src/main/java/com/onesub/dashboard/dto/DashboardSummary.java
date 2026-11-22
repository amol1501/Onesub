package com.onesub.dashboard.dto;

import com.onesub.subscription.dto.SubscriptionResponse;
import com.onesub.payment.dto.PaymentResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardSummary(
        long activeSubscriptions,
        Map<String, BigDecimal> monthlyRecurringExpenditure,
        Map<String, BigDecimal> annualEstimatedExpenditure,
        List<SubscriptionResponse> upcomingRenewals,
        List<PaymentResponse> recentPayments,
        Map<String, Long> merchantBreakdown
) {
}
