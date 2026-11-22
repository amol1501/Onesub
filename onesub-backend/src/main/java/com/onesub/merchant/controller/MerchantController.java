package com.onesub.merchant.controller;

import com.onesub.auth.UserPrincipal;
import com.onesub.common.PageResponse;
import com.onesub.merchant.dto.MerchantRequest;
import com.onesub.merchant.dto.MerchantResponse;
import com.onesub.merchant.service.MerchantService;
import com.onesub.response.ApiResponse;
import com.onesub.user.entity.User;
import com.onesub.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {
    private final MerchantService merchantService;
    private final UserRepository userRepository;

    public MerchantController(MerchantService merchantService, UserRepository userRepository) {
        this.merchantService = merchantService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<PageResponse<MerchantResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ApiResponse.success(PageResponse.from(
                merchantService.list(getUser(principal), pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<MerchantResponse> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.success(merchantService.get(id, getUser(principal)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MerchantResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MerchantRequest request) {
        return ApiResponse.success("Merchant created",
                merchantService.create(getUser(principal), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MerchantResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody MerchantRequest request) {
        return ApiResponse.success("Merchant updated",
                merchantService.update(id, getUser(principal), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        merchantService.delete(id, getUser(principal));
    }

    private User getUser(UserPrincipal principal) {
        return principal.getUser();
    }
}
