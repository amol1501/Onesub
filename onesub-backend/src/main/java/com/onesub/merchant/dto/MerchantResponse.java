package com.onesub.merchant.dto;

import com.onesub.merchant.entity.Merchant;

public record MerchantResponse(
        Long id,
        String name,
        String category,
        String websiteUrl,
        String logoUrl,
        boolean isActive,
        boolean custom
) {
    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getName(),
                merchant.getCategory(),
                merchant.getWebsiteUrl(),
                merchant.getLogoUrl(),
                merchant.isActive(),
                merchant.getOwner() != null
        );
    }
}
