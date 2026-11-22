package com.onesub.auth;

import com.onesub.auth.dto.RegisterRequest;
import com.onesub.auth.service.AuthService;
import com.onesub.exception.DuplicateResourceException;
import com.onesub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    @Test
    void registersNormalizedUserAndHashesPassword() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("{bcrypt}hash");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            com.onesub.user.entity.User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        AuthService service = new AuthService(userRepository, passwordEncoder, authenticationManager);
        var response = service.register(new RegisterRequest(
                "  Priya Patel  ", "  USER@example.com ", "password1", "Asia/Kolkata", "INR"));

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.fullName()).isEqualTo("Priya Patel");
        assertThat(response.currency()).isEqualTo("INR");
        verify(passwordEncoder).encode("password1");
        verify(userRepository).save(any());
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);
        AuthService service = new AuthService(userRepository, passwordEncoder, authenticationManager);

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "Priya", "existing@example.com", "password1", null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
