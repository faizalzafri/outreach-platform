package com.outreach.platform.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Composite Logback converter that chains PII and secret masking on log messages.
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
