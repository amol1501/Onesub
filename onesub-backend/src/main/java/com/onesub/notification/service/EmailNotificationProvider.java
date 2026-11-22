package com.onesub.notification.service;

public interface EmailNotificationProvider {
    void send(String recipientEmail, String subject, String body);
}
