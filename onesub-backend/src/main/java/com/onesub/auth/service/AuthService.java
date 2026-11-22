package com.onesub.auth.service;

import com.onesub.auth.dto.LoginRequest;
import com.onesub.auth.dto.RegisterRequest;
import com.onesub.enums.UserStatus;
import com.onesub.exception.DuplicateResourceException;
import com.onesub.user.dto.UserResponse;
import com.onesub.user.entity.User;
import com.onesub.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.ZoneId;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account already exists with this email");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setTimezone(request.timezone() == null || request.timezone().isBlank()
                ? "UTC" : request.timezone().trim());
        validateTimezone(user.getTimezone());
        user.setCurrency(request.currency() == null || request.currency().isBlank()
                ? "USD" : request.currency().trim());
        user.setStatus(UserStatus.ACTIVE);
        return UserResponse.from(userRepository.save(user));
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid timezone");
        }
    }

    public Authentication authenticate(LoginRequest request) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(), request.password()));
    }
}
