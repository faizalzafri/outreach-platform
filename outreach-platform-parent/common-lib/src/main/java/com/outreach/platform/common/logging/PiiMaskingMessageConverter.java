package com.outreach.platform.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * Logback converter that detects and redacts PII patterns (emails, phone numbers, employee IDs) in log messages.
 */
public class PiiMaskingMessageConverter extends ClassicConverter {

    private static final String REDACTED = "***REDACTED***";

    // Email pattern: local-part@domain (RFC 5322 simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
    );

    // Phone patterns: +1-555-123-4567, (555) 123-4567, 555.123.4567, 5551234567
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(\\+?\\d{1,3}[\\s\\-.]?)?" +
            "(\\(?\\d{3}\\)?[\\s\\-.]?)" +
            "\\d{3}[\\s\\-.]?" +
            "\\d{4}"
    );

    // Employee ID pattern: EMP-12345 or EMP12345 (3+ digits)
    private static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile(
            "\\bEMP-?\\d{3,}\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null || message.isEmpty()) {
            return message;
        }
        return maskPii(message);
    }

    /** Applies all PII masking patterns to the given text. */
    public static String maskPii(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String masked = EMAIL_PATTERN.matcher(text).replaceAll(REDACTED);
        masked = PHONE_PATTERN.matcher(masked).replaceAll(REDACTED);
        masked = EMPLOYEE_ID_PATTERN.matcher(masked).replaceAll(REDACTED);
        return masked;
    }
}
