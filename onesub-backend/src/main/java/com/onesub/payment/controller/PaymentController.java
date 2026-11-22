package com.onesub.payment.controller;

import com.onesub.auth.UserPrincipal;
import com.onesub.common.PageResponse;
import com.onesub.payment.dto.PaymentRequest;
import com.onesub.payment.dto.PaymentResponse;
import com.onesub.payment.service.PaymentService;
import com.onesub.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/subscriptions/{id}/payments")
    public ApiResponse<PageResponse<PaymentResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long subscriptionId,
            @PageableDefault(size = 20, sort = "paymentDate") Pageable pageable) {
        return ApiResponse.success(PageResponse.from(
                paymentService.list(subscriptionId, principal.getUser(), pageable)));
    }

    @PostMapping("/subscriptions/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long subscriptionId,
            @Valid @RequestBody PaymentRequest request) {
        return ApiResponse.success("Payment record created",
                paymentService.create(subscriptionId, principal.getUser(), request));
    }

    @PutMapping("/payments/{id}")
    public ApiResponse<PaymentResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long paymentId,
            @Valid @RequestBody PaymentRequest request) {
        return ApiResponse.success("Payment record updated",
                paymentService.update(paymentId, principal.getUser(), request));
    }

    @DeleteMapping("/payments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long paymentId) {
        paymentService.delete(paymentId, principal.getUser());
    }
}
