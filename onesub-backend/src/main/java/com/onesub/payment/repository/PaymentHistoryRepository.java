package com.onesub.payment.repository;

import com.onesub.payment.entity.PaymentHistory;
import com.onesub.subscription.entity.UserSubscription;
import com.onesub.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findBySubscription(UserSubscription subscription);
    Page<PaymentHistory> findBySubscription(UserSubscription subscription, Pageable pageable);
    Optional<PaymentHistory> findByIdAndSubscription(Long id, UserSubscription subscription);
    Optional<PaymentHistory> findByIdAndSubscription_User_Id(Long id, Long userId);
    @Query("""
            select p from PaymentHistory p
            where p.subscription.user = :user
              and p.paymentStatus = com.onesub.enums.PaymentStatus.PAID
            order by p.paymentDate desc
            """)
    Page<PaymentHistory> recentPaidForUser(
            @Param("user") User user,
            Pageable pageable);

    @Query("""
            select p.currency as currency,
                   sum(p.amount) as amount,
                   substring(cast(p.paymentDate as string), 1, 7) as period
            from PaymentHistory p
            where p.subscription.user = :user
              and p.paymentStatus = com.onesub.enums.PaymentStatus.PAID
              and p.paymentDate >= :from
              and p.paymentDate < :to
            group by p.currency, substring(cast(p.paymentDate as string), 1, 7)
            """)
    List<PaymentAggregateProjection> paidSpendByMonth(
            @Param("user") User user,
            @Param("from") java.time.LocalDate from,
            @Param("to") java.time.LocalDate to);

    interface PaymentAggregateProjection {
        String getCurrency();
        java.math.BigDecimal getAmount();
        String getPeriod();
    }
}
