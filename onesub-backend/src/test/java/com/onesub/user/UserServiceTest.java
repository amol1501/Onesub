package com.onesub.user;

import com.onesub.user.dto.UpdateUserRequest;
import com.onesub.user.entity.User;
import com.onesub.user.repository.UserRepository;
import com.onesub.user.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {
    @Test
    void updatesOnlyOwnedUserProfile() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User();
        user.setId(7L);
        user.setFullName("Old Name");
        user.setTimezone("UTC");
        user.setCurrency("USD");
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new UserService(repository).update(7L,
                new UpdateUserRequest("New Name", "Asia/Kolkata", "INR"));

        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(response.timezone()).isEqualTo("Asia/Kolkata");
        assertThat(response.currency()).isEqualTo("INR");
    }
}
