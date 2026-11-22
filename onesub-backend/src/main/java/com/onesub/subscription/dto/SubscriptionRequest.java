package com.onesub.subscription.dto;

import com.onesub.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRequest(
        @NotNull Long merchantId,
        Long planId,
        @Size(max = 100) String customPlanName,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull BillingCycle billingCycle,
        @NotNull LocalDate startDate,
        LocalDate nextBillingDate,
        LocalDate endDate,
        Boolean reminderEnabled
) {
}
