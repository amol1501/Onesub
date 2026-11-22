package com.onesub.dashboard;

import com.onesub.dashboard.service.DashboardService;
import com.onesub.enums.BillingCycle;
import com.onesub.enums.SubscriptionStatus;
import com.onesub.merchant.entity.Merchant;
import com.onesub.payment.repository.PaymentHistoryRepository;
import com.onesub.subscription.entity.UserSubscription;
import com.onesub.subscription.repository.UserSubscriptionRepository;
import com.onesub.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {
    @Test
    void calculatesMonthlyAndAnnualTotalsWithoutMixingCurrencies() {
        UserSubscriptionRepository subscriptions = mock(UserSubscriptionRepository.class);
        PaymentHistoryRepository payments = mock(PaymentHistoryRepository.class);
        User user = new User();
        user.setId(1L);

        Merchant merchant = new Merchant();
        merchant.setName("Netflix");

        UserSubscription monthlyUsd = new UserSubscription();
        monthlyUsd.setUser(user);
        monthlyUsd.setMerchant(merchant);
        monthlyUsd.setAmount(new BigDecimal("10.00"));
        monthlyUsd.setCurrency("USD");
        monthlyUsd.setBillingCycle(BillingCycle.MONTHLY);
        monthlyUsd.setNextBillingDate(LocalDate.now().plusDays(5));
        monthlyUsd.setStatus(SubscriptionStatus.ACTIVE);

        UserSubscription yearlyUsd = new UserSubscription();
        yearlyUsd.setUser(user);
        yearlyUsd.setMerchant(merchant);
        yearlyUsd.setAmount(new BigDecimal("120.00"));
        yearlyUsd.setCurrency("USD");
        yearlyUsd.setBillingCycle(BillingCycle.YEARLY);
        yearlyUsd.setNextBillingDate(LocalDate.now().plusDays(10));
        yearlyUsd.setStatus(SubscriptionStatus.ACTIVE);

        UserSubscription monthlyInr = new UserSubscription();
        monthlyInr.setUser(user);
        monthlyInr.setMerchant(merchant);
        monthlyInr.setAmount(new BigDecimal("500.00"));
        monthlyInr.setCurrency("INR");
        monthlyInr.setBillingCycle(BillingCycle.MONTHLY);
        monthlyInr.setNextBillingDate(LocalDate.now().plusDays(15));
        monthlyInr.setStatus(SubscriptionStatus.ACTIVE);

        when(subscriptions.findByUserAndStatus(user, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(monthlyUsd, yearlyUsd, monthlyInr));
        when(payments.recentPaidForUser(
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var summary = new DashboardService(subscriptions, payments)
                .summary(user);

        assertThat(summary.monthlyRecurringExpenditure())
                .containsEntry("USD", new BigDecimal("20.00"));
        assertThat(summary.monthlyRecurringExpenditure())
                .containsEntry("INR", new BigDecimal("500.00"));
        assertThat(summary.annualEstimatedExpenditure())
                .containsEntry("USD", new BigDecimal("240.00"));
    }
}
