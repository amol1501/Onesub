package com.onesub.notification.repository;

import com.onesub.notification.entity.Notification;
import com.onesub.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByScheduledAtDesc(User user);
    org.springframework.data.domain.Page<Notification> findByUser(User user,
            org.springframework.data.domain.Pageable pageable);
    java.util.Optional<Notification> findByIdAndUser(Long id, User user);
    boolean existsBySubscriptionAndNotificationTypeAndScheduledAt(
            com.onesub.subscription.entity.UserSubscription subscription,
            String notificationType,
            java.time.Instant scheduledAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select n from Notification n
            where n.status in (
                com.onesub.enums.NotificationStatus.PENDING,
                com.onesub.enums.NotificationStatus.FAILED
            )
              and n.scheduledAt <= :now
              and n.attemptCount < :maxAttempts
            order by n.scheduledAt
            """)
    List<Notification> claimDue(
            @Param("now") java.time.Instant now,
            @Param("maxAttempts") int maxAttempts,
            org.springframework.data.domain.Pageable pageable);
}
