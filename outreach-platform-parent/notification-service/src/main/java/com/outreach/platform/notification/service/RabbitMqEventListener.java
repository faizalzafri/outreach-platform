package com.outreach.platform.notification.service;

import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** RabbitMQ listener that routes domain events to notification handlers. */
@Service
public class RabbitMqEventListener {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventListener.class);

    private final EmailDeliveryRepository emailDeliveryRepository;
    private final EmailDispatchService emailDispatchService;
    private final IdentityNotificationService identityNotifications;

    @Inject
    public RabbitMqEventListener(EmailDeliveryRepository emailDeliveryRepository,
                                 EmailDispatchService emailDispatchService,
                                 IdentityNotificationService identityNotifications) {
        this.emailDeliveryRepository = emailDeliveryRepository;
        this.emailDispatchService = emailDispatchService;
        this.identityNotifications = identityNotifications;
    }

    /** Routes incoming domain events to the appropriate handler based on eventType. */
    @RabbitListener(queues = RabbitMqConstants.QUEUE_NOTIFICATION)
    public void onMessage(DomainEventMessage message) {
        log.info("Received domain event via RabbitMQ: type={}, eventId={}",
                message.eventType(), message.eventId());

        try {
            if (identityNotifications.handles(message.eventType())) {
                identityNotifications.handle(message);
                return;
            }
            switch (message.eventType()) {
                case "SendFeedbackEmails" -> handleSendFeedbackEmails(message);
                case "EventStatusChanged" -> handleEventStatusChanged(message);
                case "VolunteersImported" -> handleVolunteersImported(message);
                default -> log.warn("Unrecognized event type from RabbitMQ: {}", message.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ event: type={}, eventId={}",
                    message.eventType(), message.eventId(), e);
            throw e; // let Spring AMQP handle retry/nack
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSendFeedbackEmails(DomainEventMessage message) {
        Map<String, Object> payload = message.payload();
        String eventId = String.valueOf(payload.getOrDefault("eventId", ""));
        List<Map<String, String>> recipients = (List<Map<String, String>>) payload.getOrDefault("recipients", List.of());

        log.info("Processing SendFeedbackEmails via RabbitMQ: eventId={}, recipientCount={}", eventId, recipients.size());

        for (Map<String, String> recipient : recipients) {
            queueEmail(eventId, recipient.getOrDefault("email", ""), recipient.getOrDefault("name", ""),
                    "We'd love your feedback!",
                    "Please provide your feedback for the recent outreach event.");
        }
    }

    /** Emails the event's assigned POCs (the payload's recipients); nothing is queued if none are assigned. */
    @SuppressWarnings("unchecked")
    private void handleEventStatusChanged(DomainEventMessage message) {
        Map<String, Object> payload = message.payload();
        String eventId = String.valueOf(payload.getOrDefault("eventId", ""));
        String newStatus = String.valueOf(payload.getOrDefault("newStatus", ""));
        String eventName = String.valueOf(payload.getOrDefault("eventName", ""));
        List<Map<String, String>> recipients = (List<Map<String, String>>) payload.getOrDefault("recipients", List.of());

        log.info("Processing EventStatusChanged via RabbitMQ: eventId={}, newStatus={}, recipientCount={}",
                eventId, newStatus, recipients.size());

        for (Map<String, String> recipient : recipients) {
            queueEmail(eventId, recipient.getOrDefault("email", ""), recipient.getOrDefault("name", ""),
                    "Event Update: " + eventName + " — " + newStatus,
                    "The event '" + eventName + "' has been updated to status: " + newStatus);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleVolunteersImported(DomainEventMessage message) {
        Map<String, Object> payload = message.payload();
        String eventId = String.valueOf(payload.getOrDefault("eventId", ""));
        List<Map<String, String>> volunteers = (List<Map<String, String>>) payload.getOrDefault("volunteers", List.of());

        log.info("Processing VolunteersImported via RabbitMQ: eventId={}, volunteerCount={}", eventId, volunteers.size());

        for (Map<String, String> volunteer : volunteers) {
            queueEmail(eventId, volunteer.getOrDefault("email", ""), volunteer.getOrDefault("name", ""),
                    "Welcome to the Outreach Platform!",
                    "You have been registered as a volunteer. Thank you for your participation!");
        }
    }

    /** Persists a PENDING delivery record and hands it to the async dispatcher. */
    private void queueEmail(String eventId, String email, String name, String subject, String body) {
        EmailDeliveryDocument delivery = new EmailDeliveryDocument();
        // Bound from the message's x-tenant-id by the listener container's interceptor.
        delivery.setTenantId(TenantContext.getCurrentTenantId());
        delivery.setEventId(eventId);
        delivery.setRecipientEmail(email);
        delivery.setRecipientName(name);
        delivery.setSubject(subject);
        delivery.setBody(body);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setCreatedAt(Instant.now());

        emailDispatchService.dispatchEmail(emailDeliveryRepository.save(delivery));
    }
}
