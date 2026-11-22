package com.onesub.user.controller;

import com.onesub.auth.UserPrincipal;
import com.onesub.response.ApiResponse;
import com.onesub.user.dto.UpdateUserRequest;
import com.onesub.user.dto.UserResponse;
import com.onesub.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userService.getById(principal.getId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success("Profile updated",
                userService.update(principal.getId(), request));
    }
}
