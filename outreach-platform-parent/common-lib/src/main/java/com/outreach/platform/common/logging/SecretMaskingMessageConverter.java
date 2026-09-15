package com.outreach.platform.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * Logback converter that detects and redacts secret/credential patterns (passwords, tokens, API keys) in log messages.
 */
public class SecretMaskingMessageConverter extends ClassicConverter {

    private static final String REDACTED = "***REDACTED***";

    // Matches key=value or key="value" or key='value' patterns for secret keys
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "\\b(password|secret|apiKey|api_key|token|authorization|credential|client_secret)" +
            "\\s*[=:]\\s*" +
            "([\"']?)([^\"'\\s,;}{\\]]+)\\2",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null || message.isEmpty()) {
            return message;
        }
        return maskSecrets(message);
    }

    /** Applies secret masking patterns to the given text. */
    public static String maskSecrets(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return SECRET_PATTERN.matcher(text).replaceAll("$1=$2" + REDACTED + "$2");
    }
}
