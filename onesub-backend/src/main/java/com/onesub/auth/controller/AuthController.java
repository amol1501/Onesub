package com.onesub.auth.controller;

import com.onesub.auth.UserPrincipal;
import com.onesub.auth.dto.LoginRequest;
import com.onesub.auth.dto.RegisterRequest;
import com.onesub.auth.service.AuthService;
import com.onesub.response.ApiResponse;
import com.onesub.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public ApiResponse<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ApiResponse.success(Map.of(
                "headerName", csrfToken.getHeaderName(),
                "token", csrfToken.getToken()));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("Account created", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication authentication = authService.authenticate(request);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        httpRequest.changeSessionId();

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ApiResponse.success("Login successful", UserResponse.from(principal.getUser()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return ApiResponse.success("Logout successful", null);
    }
}
