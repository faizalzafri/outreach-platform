package com.outreach.platform.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * Logback converter that detects and redacts secret/credential patterns in log messages.
 * <p>
 * Detects common secret patterns such as:
 * <ul>
 *   <li>password=value</li>
 *   <li>secret=value</li>
 *   <li>apiKey=value</li>
 *   <li>token=value</li>
 *   <li>authorization=value</li>
 *   <li>credential=value</li>
 * </ul>
 * <p>
 * Values can be delimited by quotes, spaces, commas, semicolons, or end of string.
 * Ensures secret values are never present in log output.
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

    /**
     * Applies secret masking patterns to the given text.
     *
     * @param text the text to mask
     * @return the text with secret values replaced by REDACTED placeholder
     */
    public static String maskSecrets(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return SECRET_PATTERN.matcher(text).replaceAll("$1=$2" + REDACTED + "$2");
    }
}
