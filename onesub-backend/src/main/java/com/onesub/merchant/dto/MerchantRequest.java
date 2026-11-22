package com.onesub.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record MerchantRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 50) String category,
        @Size(max = 255) @URL String websiteUrl,
        @Size(max = 2000) String logoUrl,
        Boolean isActive
) {
}
