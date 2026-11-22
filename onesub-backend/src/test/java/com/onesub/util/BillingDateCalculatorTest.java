package com.onesub.util;

import com.onesub.enums.BillingCycle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BillingDateCalculatorTest {
    @Test
    void monthlyRenewalUsesEndOfMonthAndClampsFollowingMonths() {
        assertThat(BillingDateCalculator.calculateInitialRenewal(
                LocalDate.of(2024, 1, 31), BillingCycle.MONTHLY))
                .isEqualTo(LocalDate.of(2024, 2, 29));

        assertThat(BillingDateCalculator.calculateNext(
                LocalDate.of(2024, 2, 29), BillingCycle.MONTHLY))
                .isEqualTo(LocalDate.of(2024, 3, 29));
    }

    @Test
    void supportsWeeklyQuarterlyAndYearlyRenewals() {
        LocalDate start = LocalDate.of(2024, 1, 31);
        assertThat(BillingDateCalculator.calculateInitialRenewal(start, BillingCycle.WEEKLY))
                .isEqualTo(LocalDate.of(2024, 2, 7));
        assertThat(BillingDateCalculator.calculateInitialRenewal(start, BillingCycle.QUARTERLY))
                .isEqualTo(LocalDate.of(2024, 4, 30));
        assertThat(BillingDateCalculator.calculateInitialRenewal(start, BillingCycle.YEARLY))
                .isEqualTo(LocalDate.of(2025, 1, 31));
    }
}
