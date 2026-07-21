package com.outreach.platform.notification.service;

import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
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

/**
 * RabbitMQ listener for domain events consumed by the notification service.
 * Listens on the notification queue and routes messages to the appropriate handler.
 * The existing MongoDB poller ({@link DomainEventConsumer}) is kept as a fallback.
 */
@Service
public class RabbitMqEventListener {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventListener.class);

    private final EmailDeliveryRepository emailDeliveryRepository;
    private final EmailDispatchService emailDispatchService;

    @Inject
    public RabbitMqEventListener(EmailDeliveryRepository emailDeliveryRepository,
                                 EmailDispatchService emailDispatchService) {
        this.emailDeliveryRepository = emailDeliveryRepository;
        this.emailDispatchService = emailDispatchService;
    }

    /**
     * Handles all domain events arriving on the notification queue.
     * Routes to the appropriate handler based on eventType.
     */
    @RabbitListener(queues = RabbitMqConstants.QUEUE_NOTIFICATION)
    public void onMessage(DomainEventMessage message) {
        log.info("Received domain event via RabbitMQ: type={}, eventId={}",
                message.eventType(), message.eventId());

        try {
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
            EmailDeliveryDocument delivery = new EmailDeliveryDocument();
            delivery.setEventId(eventId);
            delivery.setRecipientEmail(recipient.getOrDefault("email", ""));
            delivery.setRecipientName(recipient.getOrDefault("name", ""));
            delivery.setSubject("We'd love your feedback!");
            delivery.setBody("Please provide your feedback for the recent outreach event.");
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setAttempts(0);
            delivery.setCreatedAt(Instant.now());

            EmailDeliveryDocument saved = emailDeliveryRepository.save(delivery);
            emailDispatchService.dispatchEmail(saved);
        }
    }

    private void handleEventStatusChanged(DomainEventMessage message) {
        Map<String, Object> payload = message.payload();
        String eventId = String.valueOf(payload.getOrDefault("eventId", ""));
        String newStatus = String.valueOf(payload.getOrDefault("newStatus", ""));
        String eventName = String.valueOf(payload.getOrDefault("eventName", ""));

        log.info("Processing EventStatusChanged via RabbitMQ: eventId={}, newStatus={}", eventId, newStatus);

        EmailDeliveryDocument delivery = new EmailDeliveryDocument();
        delivery.setEventId(eventId);
        delivery.setRecipientEmail(String.valueOf(payload.getOrDefault("notifyEmail", "")));
        delivery.setRecipientName(String.valueOf(payload.getOrDefault("notifyName", "")));
        delivery.setSubject("Event Update: " + eventName + " — " + newStatus);
        delivery.setBody("The event '" + eventName + "' has been updated to status: " + newStatus);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setCreatedAt(Instant.now());

        EmailDeliveryDocument saved = emailDeliveryRepository.save(delivery);
        emailDispatchService.dispatchEmail(saved);
    }

    @SuppressWarnings("unchecked")
    private void handleVolunteersImported(DomainEventMessage message) {
        Map<String, Object> payload = message.payload();
        String eventId = String.valueOf(payload.getOrDefault("eventId", ""));
        List<Map<String, String>> volunteers = (List<Map<String, String>>) payload.getOrDefault("volunteers", List.of());

        log.info("Processing VolunteersImported via RabbitMQ: eventId={}, volunteerCount={}", eventId, volunteers.size());

        for (Map<String, String> volunteer : volunteers) {
            EmailDeliveryDocument delivery = new EmailDeliveryDocument();
            delivery.setEventId(eventId);
            delivery.setRecipientEmail(volunteer.getOrDefault("email", ""));
            delivery.setRecipientName(volunteer.getOrDefault("name", ""));
            delivery.setSubject("Welcome to the Outreach Platform!");
            delivery.setBody("You have been registered as a volunteer. Thank you for your participation!");
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setAttempts(0);
            delivery.setCreatedAt(Instant.now());

            EmailDeliveryDocument saved = emailDeliveryRepository.save(delivery);
            emailDispatchService.dispatchEmail(saved);
        }
    }
}
