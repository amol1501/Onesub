package com.onesub.subscription.service;

import com.onesub.enums.SubscriptionStatus;
import com.onesub.exception.ResourceNotFoundException;
import com.onesub.exception.UnauthorizedException;
import com.onesub.merchant.entity.Merchant;
import com.onesub.merchant.repository.MerchantRepository;
import com.onesub.subscription.dto.SubscriptionRequest;
import com.onesub.subscription.dto.SubscriptionResponse;
import com.onesub.subscription.entity.SubscriptionPlan;
import com.onesub.subscription.entity.UserSubscription;
import com.onesub.subscription.repository.SubscriptionPlanRepository;
import com.onesub.subscription.repository.UserSubscriptionRepository;
import com.onesub.user.entity.User;
import com.onesub.util.BillingDateCalculator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SubscriptionService {
    private final UserSubscriptionRepository subscriptionRepository;
    private final MerchantRepository merchantRepository;
    private final SubscriptionPlanRepository planRepository;

    public SubscriptionService(UserSubscriptionRepository subscriptionRepository,
                               MerchantRepository merchantRepository,
                               SubscriptionPlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.merchantRepository = merchantRepository;
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> list(User user, Pageable pageable) {
        return subscriptionRepository.findByUser(user, pageable).map(SubscriptionResponse::from);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse get(Long id, User user) {
        return SubscriptionResponse.from(findOwned(id, user));
    }

    @Transactional
    public SubscriptionResponse create(User user, SubscriptionRequest request) {
        Merchant merchant = merchantRepository.findByIdAndOwnerIsNull(request.merchantId())
                .or(() -> merchantRepository.findByIdAndOwner(request.merchantId(), user))
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
        if (!merchant.isActive()) {
            throw new IllegalArgumentException("Merchant is not active");
        }

        SubscriptionPlan plan = findAndValidatePlan(request.planId(), merchant);
        validateDates(request);

        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setMerchant(merchant);
        subscription.setPlan(plan);
        subscription.setCustomPlanName(request.customPlanName() == null
                ? null : request.customPlanName().trim());
        subscription.setAmount(request.amount());
        subscription.setCurrency(request.currency());
        subscription.setBillingCycle(request.billingCycle());
        subscription.setStartDate(request.startDate());
        subscription.setNextBillingDate(request.nextBillingDate() == null
                ? BillingDateCalculator.calculateInitialRenewal(request.startDate(), request.billingCycle())
                : request.nextBillingDate());
        subscription.setEndDate(request.endDate());
        subscription.setReminderEnabled(request.reminderEnabled() == null || request.reminderEnabled());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse update(Long id, User user, SubscriptionRequest request) {
        UserSubscription subscription = findOwned(id, user);
        Merchant merchant = merchantRepository.findByIdAndOwnerIsNull(request.merchantId())
                .or(() -> merchantRepository.findByIdAndOwner(request.merchantId(), user))
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
        SubscriptionPlan plan = findAndValidatePlan(request.planId(), merchant);
        validateDates(request);

        subscription.setMerchant(merchant);
        subscription.setPlan(plan);
        subscription.setCustomPlanName(request.customPlanName() == null
                ? null : request.customPlanName().trim());
        subscription.setAmount(request.amount());
        subscription.setCurrency(request.currency());
        subscription.setBillingCycle(request.billingCycle());
        subscription.setStartDate(request.startDate());
        subscription.setNextBillingDate(request.nextBillingDate() == null
                ? BillingDateCalculator.calculateInitialRenewal(request.startDate(), request.billingCycle())
                : request.nextBillingDate());
        subscription.setEndDate(request.endDate());
        subscription.setReminderEnabled(request.reminderEnabled() == null || request.reminderEnabled());
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse pause(Long id, User user) {
        UserSubscription subscription = findOwned(id, user);
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED
                || subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new IllegalArgumentException("Cancelled or expired subscriptions cannot be paused");
        }
        subscription.setStatus(SubscriptionStatus.PAUSED);
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse resume(Long id, User user) {
        UserSubscription subscription = findOwned(id, user);
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED
                || subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new IllegalArgumentException("Cancelled or expired subscriptions cannot be resumed");
        }
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse cancel(Long id, User user) {
        UserSubscription subscription = findOwned(id, user);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setReminderEnabled(false);
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    private SubscriptionPlan findAndValidatePlan(Long planId, Merchant merchant) {
        if (planId == null) {
            return null;
        }
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));
        if (!plan.getMerchant().getId().equals(merchant.getId())) {
            throw new IllegalArgumentException("Plan does not belong to the selected merchant");
        }
        return plan;
    }

    private void validateDates(SubscriptionRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        LocalDate next = request.nextBillingDate();
        if (next != null && next.isBefore(request.startDate())) {
            throw new IllegalArgumentException("Next billing date cannot be before start date");
        }
    }

    private UserSubscription findOwned(Long id, User user) {
        return subscriptionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new UnauthorizedException("Subscription not found"));
    }
}
