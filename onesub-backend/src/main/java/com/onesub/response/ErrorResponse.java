package com.onesub.response;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        boolean success,
        String message,
        String errorCode,
        Map<String, String> validationErrors,
        Instant timestamp
) {
}
