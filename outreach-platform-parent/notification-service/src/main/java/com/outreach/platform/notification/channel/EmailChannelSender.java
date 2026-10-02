package com.outreach.platform.notification.channel;

import com.outreach.platform.notification.config.NotificationServiceProperties;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

/** Sends HTML email (with a plain-text alternative) over SMTP. */
@Component
public class EmailChannelSender implements MessageChannelSender {

    public static final String CHANNEL = "EMAIL";

    private final JavaMailSender mailSender;
    private final NotificationServiceProperties properties;

    @Inject
    public EmailChannelSender(JavaMailSender mailSender, NotificationServiceProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public void send(OutboundMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(properties.fromAddress(), properties.fromName());
            helper.setTo(message.address());
            helper.setSubject(message.subject());
            helper.setText(message.textBody(), message.htmlBody());
            if (message.attachment() != null) {
                helper.addAttachment(message.attachment().fileName(),
                        new ByteArrayResource(message.attachment().content()));
            }
            mailSender.send(mime);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            throw new ChannelDeliveryException("Email delivery failed", e);
        }
    }
}
