package com.onesub.dashboard.service;

import com.onesub.dashboard.dto.DashboardSummary;
import com.onesub.enums.SubscriptionStatus;
import com.onesub.payment.dto.PaymentResponse;
import com.onesub.payment.repository.PaymentHistoryRepository;
import com.onesub.subscription.dto.SubscriptionResponse;
import com.onesub.subscription.entity.UserSubscription;
import com.onesub.subscription.repository.UserSubscriptionRepository;
import com.onesub.user.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final UserSubscriptionRepository subscriptionRepository;
    private final PaymentHistoryRepository paymentRepository;

    public DashboardService(UserSubscriptionRepository subscriptionRepository,
                            PaymentHistoryRepository paymentRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(User user) {
        List<UserSubscription> active = subscriptionRepository.findByUserAndStatus(
                user, SubscriptionStatus.ACTIVE);
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        Map<String, BigDecimal> annual = new LinkedHashMap<>();

        for (UserSubscription subscription : active) {
            BigDecimal amount = subscription.getAmount();
            BigDecimal monthlyAmount = switch (subscription.getBillingCycle()) {
                case WEEKLY -> amount.multiply(BigDecimal.valueOf(52))
                        .divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
                case MONTHLY -> amount;
                case QUARTERLY -> amount.divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);
                case YEARLY -> amount.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
            };
            monthly.merge(subscription.getCurrency(), monthlyAmount, BigDecimal::add);

            BigDecimal yearly = switch (subscription.getBillingCycle()) {
                case WEEKLY -> amount.multiply(BigDecimal.valueOf(52));
                case MONTHLY -> amount.multiply(BigDecimal.valueOf(12));
                case QUARTERLY -> amount.multiply(BigDecimal.valueOf(4));
                case YEARLY -> amount;
            };
            annual.merge(subscription.getCurrency(), yearly, BigDecimal::add);
        }

        LocalDate today = LocalDate.now();
        List<SubscriptionResponse> upcoming = active.stream()
                .filter(s -> !s.getNextBillingDate().isBefore(today))
                .filter(s -> s.getNextBillingDate().isBefore(today.plusDays(31)))
                .sorted((a, b) -> a.getNextBillingDate().compareTo(b.getNextBillingDate()))
                .map(SubscriptionResponse::from)
                .toList();

        List<PaymentResponse> recent = paymentRepository
                .recentPaidForUser(user, PageRequest.of(0, 5))
                .getContent().stream().map(PaymentResponse::from).toList();

        Map<String, Long> merchants = active.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMerchant().getName(),
                        HashMap::new,
                        Collectors.counting()));

        return new DashboardSummary(
                active.size(), monthly, annual, upcoming, recent, merchants);
    }
}
