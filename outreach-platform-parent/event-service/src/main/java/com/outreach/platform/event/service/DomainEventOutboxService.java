package com.outreach.platform.event.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import com.outreach.platform.event.model.DomainEventDocument;
import com.outreach.platform.event.model.DomainEventStatus;
import com.outreach.platform.event.repo.DomainEventRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Outbox service for domain events. Persists events as PENDING and includes a
 * scheduled poller that publishes them to RabbitMQ.
 */
@Service
public class DomainEventOutboxService {

    private static final Logger log = LoggerFactory.getLogger(DomainEventOutboxService.class);
    private static final int MAX_RETRY_COUNT = 3;

    private final DomainEventRepository domainEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Inject
    public DomainEventOutboxService(DomainEventRepository domainEventRepository,
                                    RabbitTemplate rabbitTemplate,
                                    ObjectMapper objectMapper) {
        this.domainEventRepository = domainEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Saves a domain event to the outbox with PENDING status.
     *
     * @param eventType the type of domain event (e.g., "EventStatusChanged")
     * @param payload   the serialized event payload (JSON string)
     * @return the persisted document
     */
    public DomainEventDocument save(String eventType, String payload) {
        DomainEventDocument document = new DomainEventDocument(eventType, payload);
        DomainEventDocument saved = domainEventRepository.save(document);
        log.info("Domain event saved to outbox: type={}, id={}", eventType, saved.getId());
        return saved;
    }

    /**
     * Scheduled outbox poller that processes PENDING domain events.
     * Runs at a fixed rate (default 5 seconds). For each pending event, attempts
     * to publish it to RabbitMQ. On success marks as PUBLISHED; on repeated failure marks as FAILED.
     */
    @Scheduled(fixedRateString = "${event-service.outbox.poll-interval-ms:5000}")
    public void pollAndPublishPendingEvents() {
        List<DomainEventDocument> pendingEvents =
                domainEventRepository.findByStatusOrderByCreatedAtAsc(DomainEventStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Outbox poller found {} pending event(s)", pendingEvents.size());

        for (DomainEventDocument event : pendingEvents) {
            try {
                publishEvent(event);
                event.setStatus(DomainEventStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                domainEventRepository.save(event);
                log.info("Domain event published to RabbitMQ: type={}, id={}", event.getEventType(), event.getId());
            } catch (Exception ex) {
                handlePublishFailure(event, ex);
            }
        }
    }

    /**
     * Publishes a domain event to RabbitMQ via the outreach.events topic exchange.
     */
    private void publishEvent(DomainEventDocument event) {
        String routingKey = mapEventTypeToRoutingKey(event.getEventType());
        Map<String, Object> payload = deserializePayload(event.getPayload());

        DomainEventMessage message = new DomainEventMessage(
                event.getId(),
                event.getEventType(),
                payload,
                event.getCreatedAt()
        );

        rabbitTemplate.convertAndSend(
                RabbitMqConstants.EXCHANGE_OUTREACH_EVENTS,
                routingKey,
                message
        );

        log.debug("Published domain event to RabbitMQ: type={}, id={}, routingKey={}",
                event.getEventType(), event.getId(), routingKey);
    }

    private String mapEventTypeToRoutingKey(String eventType) {
        return switch (eventType) {
            case "EventStatusChanged" -> RabbitMqConstants.ROUTING_KEY_EVENT_STATUS_CHANGED;
            case "VolunteersImported" -> RabbitMqConstants.ROUTING_KEY_VOLUNTEERS_IMPORTED;
            case "SendFeedbackEmails" -> RabbitMqConstants.ROUTING_KEY_SEND_FEEDBACK_EMAILS;
            case "ImportJobCompleted" -> RabbitMqConstants.ROUTING_KEY_IMPORT_JOB_COMPLETED;
            default -> "event." + eventType.toLowerCase();
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializePayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize event payload as JSON map, wrapping as raw: {}", e.getMessage());
            return Map.of("raw", payload);
        }
    }

    private void handlePublishFailure(DomainEventDocument event, Exception ex) {
        int newRetryCount = event.getRetryCount() + 1;
        event.setRetryCount(newRetryCount);

        if (newRetryCount >= MAX_RETRY_COUNT) {
            event.setStatus(DomainEventStatus.FAILED);
            log.error("Domain event FAILED after {} retries: type={}, id={}",
                    MAX_RETRY_COUNT, event.getEventType(), event.getId(), ex);
        } else {
            log.warn("Domain event publish attempt {} failed: type={}, id={}",
                    newRetryCount, event.getEventType(), event.getId(), ex);
        }

        domainEventRepository.save(event);
    }
}
