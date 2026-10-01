package com.outreach.platform.notification.service;

import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.notification.channel.EmailChannelSender;
import com.outreach.platform.notification.channel.MessageChannels;
import com.outreach.platform.notification.channel.OutboundMessage;
import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Delivers account messages published by auth-service: invitations, password-reset links,
 * password-changed notices and one-time codes.
 *
 * <p>These carry secrets, so they are sent straight away rather than stored for the async
 * dispatcher: the delivery record keeps recipient, subject and outcome with the body redacted. A
 * failed send is rethrown so the message is dead-lettered for inspection; nothing here re-sends
 * a secret later.
 */
@Service
public class IdentityNotificationService {

    private static final Logger log = LoggerFactory.getLogger(IdentityNotificationService.class);
    static final String DELIVERY_EVENT_ID = "account";

    private final MessageChannels channels;
    private final EmailDeliveryRepository deliveries;

    @Inject
    public IdentityNotificationService(MessageChannels channels, EmailDeliveryRepository deliveries) {
        this.channels = channels;
        this.deliveries = deliveries;
    }

    public boolean handles(String eventType) {
        return eventType != null && eventType.startsWith("identity.");
    }

    public void handle(DomainEventMessage event) {
        Map<String, Object> p = event.payload();
        String channel = String.valueOf(p.getOrDefault("channel", EmailChannelSender.CHANNEL));
        // Email is the only implemented channel; an SMS/push event would carry its own address field.
        String address = p.get("email") == null ? null : p.get("email").toString();
        if (address == null || address.isBlank()) {
            log.warn("Account message has no address, skipped: type={}, eventId={}", event.eventType(), event.eventId());
            return;
        }

        OutboundMessage message = switch (event.eventType()) {
            case RabbitMqConstants.ROUTING_KEY_IDENTITY_USER_INVITED -> IdentityMessages.invitation(address, p);
            case RabbitMqConstants.ROUTING_KEY_IDENTITY_PASSWORD_RESET_REQUESTED -> IdentityMessages.passwordReset(address, p);
            case RabbitMqConstants.ROUTING_KEY_IDENTITY_PASSWORD_CHANGED -> IdentityMessages.passwordChanged(address, p);
            case RabbitMqConstants.ROUTING_KEY_IDENTITY_OTP_ISSUED -> IdentityMessages.oneTimeCode(address, p);
            default -> null;
        };
        if (message == null) {
            log.warn("Unhandled account message type: {}", event.eventType());
            return;
        }

        EmailDeliveryDocument record = record(message, event.eventType());
        try {
            channels.sender(channel).send(message);
            record.setStatus(DeliveryStatus.SENT);
            record.setSentAt(Instant.now());
            deliveries.save(record);
        } catch (RuntimeException e) {
            record.setStatus(DeliveryStatus.PERMANENTLY_FAILED);
            record.setErrorMessage(e.getMessage());
            deliveries.save(record);
            throw e;
        }
    }

    private static EmailDeliveryDocument record(OutboundMessage message, String eventType) {
        EmailDeliveryDocument record = new EmailDeliveryDocument();
        record.setTenantId(TenantContext.isPresent() ? TenantContext.getCurrentTenantId() : null);
        record.setEventId(DELIVERY_EVENT_ID);
        record.setRecipientEmail(message.address());
        record.setRecipientName(message.recipientName());
        record.setSubject(eventType.equals(RabbitMqConstants.ROUTING_KEY_IDENTITY_OTP_ISSUED)
                ? "Verification code" : message.subject());
        record.setBody("[not stored: contains a single-use link or code]");
        record.setRedacted(true);
        record.setAttempts(1);
        record.setCreatedAt(Instant.now());
        record.setLastAttemptAt(Instant.now());
        return record;
    }
}
