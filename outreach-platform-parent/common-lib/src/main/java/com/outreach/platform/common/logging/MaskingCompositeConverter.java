package com.outreach.platform.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Composite Logback converter that applies both PII and secret masking to log messages.
 * <p>
 * This converter chains:
 * <ol>
 *   <li>{@link PiiMaskingMessageConverter} — redacts emails, phone numbers, employee IDs</li>
 *   <li>{@link SecretMaskingMessageConverter} — redacts passwords, tokens, API keys</li>
 * </ol>
 * <p>
 * Register in logback-spring.xml as a custom conversion word to use in pattern layouts.
 * Ensures PII and secrets are masked in pattern-layout log output.
 */
public class MaskingCompositeConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null || message.isEmpty()) {
            return message;
        }
        String masked = PiiMaskingMessageConverter.maskPii(message);
        return SecretMaskingMessageConverter.maskSecrets(masked);
    }
}
