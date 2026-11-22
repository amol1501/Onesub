package com.onesub.payment;

import com.onesub.exception.UnauthorizedException;
import com.onesub.payment.repository.PaymentHistoryRepository;
import com.onesub.payment.service.PaymentService;
import com.onesub.subscription.repository.UserSubscriptionRepository;
import com.onesub.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    @Test
    void preventsPaymentLookupThroughAnotherUsersSubscription() {
        PaymentHistoryRepository payments = mock(PaymentHistoryRepository.class);
        UserSubscriptionRepository subscriptions = mock(UserSubscriptionRepository.class);
        PaymentService service = new PaymentService(payments, subscriptions);

        User user = new User();
        user.setId(2L);
        when(subscriptions.findByIdAndUser(55L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(55L, user,
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .isInstanceOf(UnauthorizedException.class);
    }
}
