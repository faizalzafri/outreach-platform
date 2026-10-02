package com.outreach.platform.notification.channel;

/** A channel could not deliver a message (e.g. SMTP rejected it). */
public class ChannelDeliveryException extends RuntimeException {

    public ChannelDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChannelDeliveryException(String message) {
        super(message);
    }
}
