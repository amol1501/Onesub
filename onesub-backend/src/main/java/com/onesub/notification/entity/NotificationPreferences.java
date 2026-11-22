package com.onesub.notification.entity;

import com.onesub.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "reminder_7_days", nullable = false)
    private boolean reminder7Days = true;
    @Column(name = "reminder_3_days", nullable = false)
    private boolean reminder3Days = true;
    @Column(name = "reminder_1_day", nullable = false)
    private boolean reminder1Day = true;
    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public boolean isReminder7Days() { return reminder7Days; }
    public void setReminder7Days(boolean reminder7Days) { this.reminder7Days = reminder7Days; }
    public boolean isReminder3Days() { return reminder3Days; }
    public void setReminder3Days(boolean reminder3Days) { this.reminder3Days = reminder3Days; }
    public boolean isReminder1Day() { return reminder1Day; }
    public void setReminder1Day(boolean reminder1Day) { this.reminder1Day = reminder1Day; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
}
