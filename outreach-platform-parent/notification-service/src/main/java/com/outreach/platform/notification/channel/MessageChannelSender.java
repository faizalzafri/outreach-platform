package com.outreach.platform.notification.channel;

/**
 * Delivers messages over one channel. Email is implemented; SMS or push is added by writing
 * another implementation as a Spring bean. {@link MessageChannels} picks it up by {@link #channel()}.
 */
public interface MessageChannelSender {

    /** The channel name used in events, e.g. {@code EMAIL}, {@code SMS}, {@code PUSH}. */
    String channel();

    /** Sends synchronously; throws {@link ChannelDeliveryException} if the message was not accepted. */
    void send(OutboundMessage message);
}
