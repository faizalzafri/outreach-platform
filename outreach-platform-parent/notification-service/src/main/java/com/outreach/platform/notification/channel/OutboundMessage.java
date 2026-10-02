package com.outreach.platform.notification.channel;

/** A message for one recipient, independent of the channel that delivers it. */
public record OutboundMessage(String address, String recipientName, String subject, String htmlBody, String textBody,
                              Attachment attachment) {

    public OutboundMessage(String address, String recipientName, String subject, String htmlBody, String textBody) {
        this(address, recipientName, subject, htmlBody, textBody, null);
    }

    /** A file sent along with the message, where the channel supports it (email does). */
    public record Attachment(String fileName, byte[] content) {
    }
}
