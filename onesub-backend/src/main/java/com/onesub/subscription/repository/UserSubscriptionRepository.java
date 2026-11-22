package com.onesub.subscription.repository;

import com.onesub.subscription.entity.UserSubscription;
import com.onesub.user.entity.User;
import com.onesub.merchant.entity.Merchant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findByUser(User user);
    Page<UserSubscription> findByUser(User user, Pageable pageable);
    Optional<UserSubscription> findByIdAndUser(Long id, User user);
    long countByUserAndStatus(User user, com.onesub.enums.SubscriptionStatus status);
    List<UserSubscription> findByUserAndStatus(User user, com.onesub.enums.SubscriptionStatus status);
    List<UserSubscription> findByStatus(com.onesub.enums.SubscriptionStatus status);
    boolean existsByMerchant(Merchant merchant);

    @Query("""
            select s.currency as currency,
                   sum(s.amount) as monthlyAmount
            from UserSubscription s
            where s.user = :user
              and s.status = com.onesub.enums.SubscriptionStatus.ACTIVE
            group by s.currency
            """)
    List<MonthlySpendingProjection> monthlySpendingByCurrency(@Param("user") User user);

    interface MonthlySpendingProjection {
        String getCurrency();
        java.math.BigDecimal getMonthlyAmount();
    }
}
