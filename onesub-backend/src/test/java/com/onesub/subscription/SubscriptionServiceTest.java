package com.onesub.subscription;

import com.onesub.exception.UnauthorizedException;
import com.onesub.subscription.repository.SubscriptionPlanRepository;
import com.onesub.subscription.repository.UserSubscriptionRepository;
import com.onesub.subscription.service.SubscriptionService;
import com.onesub.merchant.repository.MerchantRepository;
import com.onesub.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriptionServiceTest {
    @Test
    void preventsOneUserReadingAnotherUsersSubscription() {
        UserSubscriptionRepository subscriptions = mock(UserSubscriptionRepository.class);
        MerchantRepository merchants = mock(MerchantRepository.class);
        SubscriptionPlanRepository plans = mock(SubscriptionPlanRepository.class);
        SubscriptionService service = new SubscriptionService(subscriptions, merchants, plans);

        User owner = new User();
        owner.setId(1L);
        when(subscriptions.findByIdAndUser(99L, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L, owner))
                .isInstanceOf(UnauthorizedException.class);
    }
}
