package com.outreach.platform.event.service;

import com.outreach.platform.event.model.DomainEventDocument;
import com.outreach.platform.event.model.DomainEventStatus;
import com.outreach.platform.event.repo.DomainEventRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Outbox service for domain events. Persists events as PENDING and includes a
 * scheduled poller that processes them (marks as PUBLISHED or FAILED).
 *
 * <p>In a production environment the poller would publish events to a message broker
 * (e.g., Kafka, RabbitMQ). For now it logs and marks events as PUBLISHED.</p>
 */
@Service
public class DomainEventOutboxService {

    private static final Logger log = LoggerFactory.getLogger(DomainEventOutboxService.class);
    private static final int MAX_RETRY_COUNT = 3;

    private final DomainEventRepository domainEventRepository;

    @Inject
    public DomainEventOutboxService(DomainEventRepository domainEventRepository) {
        this.domainEventRepository = domainEventRepository;
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
     * to publish it. On success marks as PUBLISHED; on repeated failure marks as FAILED.
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
                log.info("Domain event published: type={}, id={}", event.getEventType(), event.getId());
            } catch (Exception ex) {
                handlePublishFailure(event, ex);
            }
        }
    }

    /**
     * Simulates publishing a domain event. In production this would send
     * the event to a message broker.
     */
    private void publishEvent(DomainEventDocument event) {
        log.debug("Publishing domain event: type={}, id={}, payload={}",
                event.getEventType(), event.getId(), event.getPayload());
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
