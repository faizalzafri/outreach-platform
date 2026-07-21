package com.outreach.platform.notification.service;

import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.DomainEventDocument;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.DomainEventRepository;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Scheduled poller that consumes domain events from MongoDB and triggers
 * the appropriate notification workflows.
 *
 * Handles:
 *  - SendFeedbackEmails: triggers batch email dispatch for feedback collection
 *  - EventStatusChanged: triggers event lifecycle notifications
 *  - VolunteersImported: triggers welcome/confirmation emails
 */
@Service
public class DomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumer.class);

    private static final String EVENT_SEND_FEEDBACK_EMAILS = "SendFeedbackEmails";
    private static final String EVENT_STATUS_CHANGED = "EventStatusChanged";
    private static final String EVENT_VOLUNTEERS_IMPORTED = "VolunteersImported";

    private static final List<String> HANDLED_EVENTS = List.of(
            EVENT_SEND_FEEDBACK_EMAILS,
            EVENT_STATUS_CHANGED,
            EVENT_VOLUNTEERS_IMPORTED
    );

    private final DomainEventRepository domainEventRepository;
    private final EmailDeliveryRepository emailDeliveryRepository;
    private final EmailDispatchService emailDispatchService;

    @Inject
    public DomainEventConsumer(DomainEventRepository domainEventRepository,
                               EmailDeliveryRepository emailDeliveryRepository,
                               EmailDispatchService emailDispatchService) {
        this.domainEventRepository = domainEventRepository;
        this.emailDeliveryRepository = emailDeliveryRepository;
        this.emailDispatchService = emailDispatchService;
    }

    /**
     * Polls MongoDB for unprocessed domain events every 10 seconds and dispatches
     * them to the appropriate handler.
     */
    @Scheduled(fixedDelayString = "${notification-service.event-poll-interval-ms:10000}")
    public void pollDomainEvents() {
        List<DomainEventDocument> events = domainEventRepository
                .findByEventTypeInAndProcessedFalse(HANDLED_EVENTS);

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} unprocessed domain events to handle", events.size());

        for (DomainEventDocument event : events) {
            try {
                handleEvent(event);
                markProcessed(event);
            } catch (Exception e) {
                log.error("Failed to process domain event: id={}, type={}", event.getId(), event.getEventType(), e);
            }
        }
    }

    private void handleEvent(DomainEventDocument event) {
        switch (event.getEventType()) {
            case EVENT_SEND_FEEDBACK_EMAILS -> handleSendFeedbackEmails(event);
            case EVENT_STATUS_CHANGED -> handleEventStatusChanged(event);
            case EVENT_VOLUNTEERS_IMPORTED -> handleVolunteersImported(event);
            default -> log.warn("Unrecognized event type: {}", event.getEventType());
        }
    }

    /**
     * Handles SendFeedbackEmails events by creating delivery records for each recipient
     * and dispatching them via the email service.
     */
    @SuppressWarnings("unchecked")
    private void handleSendFeedbackEmails(DomainEventDocument event) {
        Map<String, Object> payload = event.getPayload();
        String eventId = (String) payload.getOrDefault("eventId", "");
        List<Map<String, String>> recipients = (List<Map<String, String>>) payload.getOrDefault("recipients", List.of());

        log.info("Processing SendFeedbackEmails: eventId={}, recipientCount={}", eventId, recipients.size());

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

    /**
     * Handles EventStatusChanged events by sending lifecycle notifications
     * (e.g., event published, cancelled, completed).
     */
    private void handleEventStatusChanged(DomainEventDocument event) {
        Map<String, Object> payload = event.getPayload();
        String eventId = (String) payload.getOrDefault("eventId", "");
        String newStatus = (String) payload.getOrDefault("newStatus", "");
        String eventName = (String) payload.getOrDefault("eventName", "");

        log.info("Processing EventStatusChanged: eventId={}, newStatus={}", eventId, newStatus);

        EmailDeliveryDocument delivery = new EmailDeliveryDocument();
        delivery.setEventId(eventId);
        delivery.setRecipientEmail((String) payload.getOrDefault("notifyEmail", ""));
        delivery.setRecipientName((String) payload.getOrDefault("notifyName", ""));
        delivery.setSubject("Event Update: " + eventName + " — " + newStatus);
        delivery.setBody("The event '" + eventName + "' has been updated to status: " + newStatus);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setCreatedAt(Instant.now());

        EmailDeliveryDocument saved = emailDeliveryRepository.save(delivery);
        emailDispatchService.dispatchEmail(saved);
    }

    /**
     * Handles VolunteersImported events by sending welcome emails to new volunteers.
     */
    @SuppressWarnings("unchecked")
    private void handleVolunteersImported(DomainEventDocument event) {
        Map<String, Object> payload = event.getPayload();
        String eventId = (String) payload.getOrDefault("eventId", "");
        List<Map<String, String>> volunteers = (List<Map<String, String>>) payload.getOrDefault("volunteers", List.of());

        log.info("Processing VolunteersImported: eventId={}, volunteerCount={}", eventId, volunteers.size());

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

    private void markProcessed(DomainEventDocument event) {
        event.setProcessed(true);
        event.setProcessedAt(Instant.now());
        domainEventRepository.save(event);
    }
}
