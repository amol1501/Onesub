package com.onesub.payment.dto;

import com.onesub.enums.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull LocalDate paymentDate,
        @NotNull PaymentStatus paymentStatus,
        @Size(max = 5000) String notes
) {
}
