package com.onesub.merchant.service;

import com.onesub.exception.DuplicateResourceException;
import com.onesub.exception.ResourceNotFoundException;
import com.onesub.exception.UnauthorizedException;
import com.onesub.merchant.dto.MerchantRequest;
import com.onesub.merchant.dto.MerchantResponse;
import com.onesub.merchant.entity.Merchant;
import com.onesub.merchant.repository.MerchantRepository;
import com.onesub.user.entity.User;
import com.onesub.subscription.repository.UserSubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantService {
    private final MerchantRepository merchantRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    public MerchantService(MerchantRepository merchantRepository,
                           UserSubscriptionRepository subscriptionRepository) {
        this.merchantRepository = merchantRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public Page<MerchantResponse> list(User owner, Pageable pageable) {
        return merchantRepository.findByOwnerIsNullOrOwner(owner, pageable)
                .map(MerchantResponse::from);
    }

    @Transactional(readOnly = true)
    public MerchantResponse get(Long id, User owner) {
        return MerchantResponse.from(findAccessible(id, owner));
    }

    @Transactional
    public MerchantResponse create(User owner, MerchantRequest request) {
        if (merchantRepository.existsByOwnerAndNameIgnoreCase(owner, request.name().trim())
                || merchantRepository.existsByOwnerIsNullAndNameIgnoreCase(request.name().trim())) {
            throw new DuplicateResourceException("A merchant with this name already exists");
        }
        Merchant merchant = new Merchant();
        applyRequest(merchant, request);
        merchant.setOwner(owner);
        merchant.setActive(request.isActive() == null || request.isActive());
        return MerchantResponse.from(merchantRepository.save(merchant));
    }

    @Transactional
    public MerchantResponse update(Long id, User owner, MerchantRequest request) {
        Merchant merchant = merchantRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new UnauthorizedException("Merchant not found or cannot be modified"));
        if (merchantRepository.existsByOwnerAndNameIgnoreCase(owner, request.name().trim())
                && !merchant.getName().equalsIgnoreCase(request.name().trim())) {
            throw new DuplicateResourceException("A merchant with this name already exists");
        }
        applyRequest(merchant, request);
        merchant.setActive(request.isActive() == null || request.isActive());
        return MerchantResponse.from(merchantRepository.save(merchant));
    }

    @Transactional
    public void delete(Long id, User owner) {
        Merchant merchant = merchantRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new UnauthorizedException("Merchant not found or cannot be modified"));
        if (subscriptionRepository.existsByMerchant(merchant)) {
            throw new IllegalArgumentException("Merchant cannot be deleted while subscriptions use it");
        }
        merchantRepository.delete(merchant);
    }

    private Merchant findAccessible(Long id, User owner) {
        return merchantRepository.findByIdAndOwnerIsNull(id)
                .or(() -> merchantRepository.findByIdAndOwner(id, owner))
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
    }

    private void applyRequest(Merchant merchant, MerchantRequest request) {
        merchant.setName(request.name().trim());
        merchant.setCategory(request.category() == null ? null : request.category().trim());
        merchant.setWebsiteUrl(request.websiteUrl() == null ? null : request.websiteUrl().trim());
        merchant.setLogoUrl(request.logoUrl() == null ? null : request.logoUrl().trim());
    }
}
