package com.onesub.payment.dto;

import com.onesub.enums.PaymentStatus;
import com.onesub.payment.entity.PaymentHistory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentResponse(
        Long id,
        Long subscriptionId,
        BigDecimal amount,
        String currency,
        LocalDate paymentDate,
        PaymentStatus paymentStatus,
        String notes
) {
    public static PaymentResponse from(PaymentHistory payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getSubscription().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentDate(),
                payment.getPaymentStatus(),
                payment.getNotes()
        );
    }
}
