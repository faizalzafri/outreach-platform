package com.outreach.platform.ingestion.service;

import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import com.outreach.platform.ingestion.model.DomainEventDocument;
import com.outreach.platform.ingestion.model.EventStatus;
import com.outreach.platform.ingestion.repo.DomainEventRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** Scheduled poller that reads PENDING domain events from MongoDB and publishes them to RabbitMQ. */
@Service
public class DomainEventOutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(DomainEventOutboxPoller.class);

    private final DomainEventRepository domainEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Inject
    public DomainEventOutboxPoller(DomainEventRepository domainEventRepository,
                                   RabbitTemplate rabbitTemplate) {
        this.domainEventRepository = domainEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Polls for PENDING events every 5 seconds and publishes them to RabbitMQ.
     */
    @Scheduled(fixedRateString = "${ingestion-service.outbox.poll-interval-ms:5000}")
    public void pollAndPublish() {
        List<DomainEventDocument> pendingEvents = domainEventRepository.findByStatus(EventStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Ingestion outbox poller found {} pending event(s)", pendingEvents.size());

        for (DomainEventDocument event : pendingEvents) {
            try {
                publishToRabbitMq(event);
                event.setStatus(EventStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                domainEventRepository.save(event);
                log.info("Ingestion domain event published to RabbitMQ: type={}, id={}",
                        event.getEventType(), event.getId());
            } catch (Exception ex) {
                log.error("Failed to publish ingestion domain event: type={}, id={}",
                        event.getEventType(), event.getId(), ex);
            }
        }
    }

    private void publishToRabbitMq(DomainEventDocument event) {
        String routingKey = mapEventTypeToRoutingKey(event.getEventType());

        DomainEventMessage message = new DomainEventMessage(
                event.getId(),
                event.getEventType(),
                event.getPayload(),
                event.getCreatedAt()
        );

        rabbitTemplate.convertAndSend(
                RabbitMqConstants.EXCHANGE_OUTREACH_EVENTS,
                routingKey,
                message
        );
    }

    private String mapEventTypeToRoutingKey(String eventType) {
        return switch (eventType) {
            case "VolunteersImported" -> RabbitMqConstants.ROUTING_KEY_VOLUNTEERS_IMPORTED;
            case "SendFeedbackEmails" -> RabbitMqConstants.ROUTING_KEY_SEND_FEEDBACK_EMAILS;
            case "ImportJobCompleted" -> RabbitMqConstants.ROUTING_KEY_IMPORT_JOB_COMPLETED;
            case "EventSummaryImported" -> "event.event-summary-imported";
            default -> "event." + eventType.toLowerCase();
        };
    }
}
