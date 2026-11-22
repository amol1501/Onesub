package com.onesub.payment.service;

import com.onesub.exception.ResourceNotFoundException;
import com.onesub.exception.UnauthorizedException;
import com.onesub.payment.dto.PaymentRequest;
import com.onesub.payment.dto.PaymentResponse;
import com.onesub.payment.entity.PaymentHistory;
import com.onesub.payment.repository.PaymentHistoryRepository;
import com.onesub.subscription.entity.UserSubscription;
import com.onesub.subscription.repository.UserSubscriptionRepository;
import com.onesub.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentHistoryRepository paymentRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    public PaymentService(PaymentHistoryRepository paymentRepository,
                          UserSubscriptionRepository subscriptionRepository) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> list(Long subscriptionId, User user, Pageable pageable) {
        UserSubscription subscription = findOwnedSubscription(subscriptionId, user);
        return paymentRepository.findBySubscription(subscription, pageable)
                .map(PaymentResponse::from);
    }

    @Transactional
    public PaymentResponse create(Long subscriptionId, User user, PaymentRequest request) {
        UserSubscription subscription = findOwnedSubscription(subscriptionId, user);
        if (!subscription.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new IllegalArgumentException("Payment currency must match the subscription currency");
        }

        PaymentHistory payment = new PaymentHistory();
        applyRequest(payment, subscription, request);
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse update(Long paymentId, User user, PaymentRequest request) {
        PaymentHistory payment = findOwnedPayment(paymentId, user);
        if (!payment.getSubscription().getCurrency().equalsIgnoreCase(request.currency())) {
            throw new IllegalArgumentException("Payment currency must match the subscription currency");
        }
        applyRequest(payment, payment.getSubscription(), request);
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(Long paymentId, User user) {
        paymentRepository.delete(findOwnedPayment(paymentId, user));
    }

    private void applyRequest(PaymentHistory payment,
                              UserSubscription subscription,
                              PaymentRequest request) {
        payment.setSubscription(subscription);
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setPaymentDate(request.paymentDate());
        payment.setPaymentStatus(request.paymentStatus());
        payment.setNotes(request.notes());
    }

    private UserSubscription findOwnedSubscription(Long id, User user) {
        return subscriptionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new UnauthorizedException("Subscription not found"));
    }

    private PaymentHistory findOwnedPayment(Long paymentId, User user) {
        return paymentRepository.findByIdAndSubscription_User_Id(paymentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));
    }
}
