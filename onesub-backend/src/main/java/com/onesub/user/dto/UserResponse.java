package com.onesub.user.dto;

import com.onesub.enums.UserStatus;
import com.onesub.user.entity.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String timezone,
        String currency,
        UserStatus status,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getTimezone(),
                user.getCurrency(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
