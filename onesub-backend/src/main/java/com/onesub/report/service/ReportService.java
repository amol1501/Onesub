package com.onesub.report.service;

import com.onesub.enums.SubscriptionStatus;
import com.onesub.report.dto.MerchantWiseReport;
import com.onesub.report.dto.SpendingReport;
import com.onesub.subscription.entity.UserSubscription;
import com.onesub.subscription.repository.UserSubscriptionRepository;
import com.onesub.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    private final UserSubscriptionRepository subscriptionRepository;

    public ReportService(UserSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public SpendingReport monthly(User user) {
        return spendingReport(user, "MONTHLY");
    }

    @Transactional(readOnly = true)
    public SpendingReport yearly(User user) {
        return spendingReport(user, "YEARLY");
    }

    @Transactional(readOnly = true)
    public MerchantWiseReport merchantWise(User user) {
        List<UserSubscription> active = subscriptionRepository.findByUserAndStatus(
                user, SubscriptionStatus.ACTIVE);
        Map<String, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        for (UserSubscription subscription : active) {
            result.computeIfAbsent(subscription.getCurrency(), key -> new LinkedHashMap<>())
                    .merge(subscription.getMerchant().getName(), subscription.getAmount(), BigDecimal::add);
        }
        return new MerchantWiseReport(result);
    }

    private SpendingReport spendingReport(User user, String period) {
        List<UserSubscription> active = subscriptionRepository.findByUserAndStatus(
                user, SubscriptionStatus.ACTIVE);
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        Map<String, BigDecimal> byMerchant = new LinkedHashMap<>();
        for (UserSubscription subscription : active) {
            BigDecimal amount = subscription.getAmount();
            BigDecimal total = "YEARLY".equals(period)
                    ? yearlyAmount(amount, subscription)
                    : monthlyAmount(amount, subscription);
            totals.merge(subscription.getCurrency(), total, BigDecimal::add);
            byMerchant.merge(subscription.getMerchant().getName(), total, BigDecimal::add);
        }
        return new SpendingReport(period, totals, byMerchant);
    }

    private BigDecimal monthlyAmount(BigDecimal amount, UserSubscription subscription) {
        return switch (subscription.getBillingCycle()) {
            case WEEKLY -> amount.multiply(BigDecimal.valueOf(52))
                    .divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
            case MONTHLY -> amount;
            case QUARTERLY -> amount.divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);
            case YEARLY -> amount.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
        };
    }

    private BigDecimal yearlyAmount(BigDecimal amount, UserSubscription subscription) {
        return switch (subscription.getBillingCycle()) {
            case WEEKLY -> amount.multiply(BigDecimal.valueOf(52));
            case MONTHLY -> amount.multiply(BigDecimal.valueOf(12));
            case QUARTERLY -> amount.multiply(BigDecimal.valueOf(4));
            case YEARLY -> amount;
        };
    }
}
