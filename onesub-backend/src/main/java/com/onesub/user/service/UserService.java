package com.onesub.user.service;

import com.onesub.exception.ResourceNotFoundException;
import com.onesub.user.dto.UpdateUserRequest;
import com.onesub.user.dto.UserResponse;
import com.onesub.user.entity.User;
import com.onesub.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(findById(id));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findById(id);
        user.setFullName(request.fullName().trim());
        if (request.timezone() != null) {
            String timezone = request.timezone().trim();
            try {
                ZoneId.of(timezone);
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Invalid timezone");
            }
            user.setTimezone(timezone);
        }
        if (request.currency() != null) {
            user.setCurrency(request.currency().trim());
        }
        return UserResponse.from(userRepository.save(user));
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
