package com.onesub.subscription.dto;

import com.onesub.enums.BillingCycle;
import com.onesub.enums.SubscriptionStatus;
import com.onesub.subscription.entity.UserSubscription;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
        Long id,
        Long merchantId,
        String merchantName,
        Long planId,
        String planName,
        BigDecimal amount,
        String currency,
        BillingCycle billingCycle,
        LocalDate startDate,
        LocalDate nextBillingDate,
        LocalDate endDate,
        SubscriptionStatus status,
        boolean reminderEnabled
) {
    public static SubscriptionResponse from(UserSubscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getMerchant().getId(),
                subscription.getMerchant().getName(),
                subscription.getPlan() == null ? null : subscription.getPlan().getId(),
                subscription.getPlan() == null
                        ? subscription.getCustomPlanName()
                        : subscription.getPlan().getPlanName(),
                subscription.getAmount(),
                subscription.getCurrency(),
                subscription.getBillingCycle(),
                subscription.getStartDate(),
                subscription.getNextBillingDate(),
                subscription.getEndDate(),
                subscription.getStatus(),
                subscription.isReminderEnabled()
        );
    }
}
