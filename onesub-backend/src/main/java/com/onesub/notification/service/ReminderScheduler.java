package com.onesub.notification.service;

import com.onesub.enums.NotificationStatus;
import com.onesub.enums.SubscriptionStatus;
import com.onesub.notification.entity.Notification;
import com.onesub.notification.entity.NotificationPreferences;
import com.onesub.notification.repository.NotificationPreferencesRepository;
import com.onesub.notification.repository.NotificationRepository;
import com.onesub.subscription.entity.UserSubscription;
import com.onesub.subscription.repository.UserSubscriptionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class ReminderScheduler {
    private final UserSubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final EmailNotificationProvider emailProvider;

    public ReminderScheduler(UserSubscriptionRepository subscriptionRepository,
                             NotificationRepository notificationRepository,
                             NotificationPreferencesRepository preferencesRepository,
                             EmailNotificationProvider emailProvider) {
        this.subscriptionRepository = subscriptionRepository;
        this.notificationRepository = notificationRepository;
        this.preferencesRepository = preferencesRepository;
        this.emailProvider = emailProvider;
    }

    @Scheduled(fixedDelayString = "${onesub.reminders.schedule-delay-ms:300000}")
    @Transactional
    public void generateAndProcessReminders() {
        for (UserSubscription subscription : subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)) {
            if (!subscription.isReminderEnabled()) {
                continue;
            }
            NotificationPreferences preferences = preferencesRepository.findByUserId(subscription.getUser().getId())
                    .orElseGet(() -> defaultPreferences(subscription));
            if (!preferences.isEmailEnabled()) {
                continue;
            }

            createReminder(subscription, preferences, 7);
            createReminder(subscription, preferences, 3);
            createReminder(subscription, preferences, 1);
        }

        List<Notification> claimed = notificationRepository.claimDue(
                Instant.now(), 3, PageRequest.of(0, 50));
        for (Notification notification : claimed) {
            notification.setStatus(NotificationStatus.CLAIMED);
            notification.setClaimedAt(Instant.now());
            notification.setAttemptCount(notification.getAttemptCount() + 1);
            notificationRepository.save(notification);
            try {
                emailProvider.send(
                        notification.getUser().getEmail(),
                        "OneSub renewal reminder",
                        "Your subscription renewal is approaching.");
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                notification.setLastError(null);
            } catch (RuntimeException ex) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setLastError(ex.getMessage());
            }
            notificationRepository.save(notification);
        }
    }

    private void createReminder(UserSubscription subscription,
                                NotificationPreferences preferences,
                                int daysBefore) {
        boolean enabled = switch (daysBefore) {
            case 7 -> preferences.isReminder7Days();
            case 3 -> preferences.isReminder3Days();
            case 1 -> preferences.isReminder1Day();
            default -> false;
        };
        if (!enabled) {
            return;
        }

        LocalDate reminderDate = subscription.getNextBillingDate().minusDays(daysBefore);
        ZoneId zone = ZoneId.of(subscription.getUser().getTimezone());
        Instant scheduledAt = ZonedDateTime.of(reminderDate, LocalTime.of(9, 0), zone).toInstant();

        String type = "RENEWAL_" + daysBefore + "_DAYS";
        if (!notificationRepository.existsBySubscriptionAndNotificationTypeAndScheduledAt(
                subscription, type, scheduledAt)) {
            Notification notification = new Notification();
            notification.setUser(subscription.getUser());
            notification.setSubscription(subscription);
            notification.setNotificationType(type);
            notification.setScheduledAt(scheduledAt);
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);
        }
    }

    private NotificationPreferences defaultPreferences(UserSubscription subscription) {
        NotificationPreferences preferences = new NotificationPreferences();
        preferences.setUserId(subscription.getUser().getId());
        return preferences;
    }
}
