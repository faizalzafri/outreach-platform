package com.outreach.platform.notification.channel;

/**
 * A message ready to deliver on one channel. {@code address} is channel-specific: an email
 * address for EMAIL, a phone number for SMS, a device token for PUSH. Channels that cannot show
 * HTML use {@code textBody}.
 */
public record OutboundMessage(String address, String recipientName, String subject, String htmlBody, String textBody) {
}
