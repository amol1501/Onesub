package com.onesub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 60) String timezone,
        @Pattern(regexp = "[A-Z]{3}") String currency
) {
}
