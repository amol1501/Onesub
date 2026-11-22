package com.onesub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "onesub.email.provider", havingValue = "log", matchIfMissing = true)
public class LoggingEmailNotificationProvider implements EmailNotificationProvider {
    private static final Logger log = LoggerFactory.getLogger(LoggingEmailNotificationProvider.class);

    @Override
    public void send(String recipientEmail, String subject, String body) {
        log.info("Development email to={} subject={} body={}", recipientEmail, subject, body);
    }
}
