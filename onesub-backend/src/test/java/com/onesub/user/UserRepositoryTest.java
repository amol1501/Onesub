package com.onesub.user;

import com.onesub.user.entity.User;
import com.onesub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void findsUserByEmailIgnoringCase() {
        User user = new User();
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("{bcrypt}hash");
        user.setTimezone("UTC");
        user.setCurrency("USD");
        userRepository.saveAndFlush(user);

        assertThat(userRepository.findByEmailIgnoreCase("TEST@EXAMPLE.COM"))
                .isPresent()
                .get()
                .extracting(User::getFullName)
                .isEqualTo("Test User");
    }
}
