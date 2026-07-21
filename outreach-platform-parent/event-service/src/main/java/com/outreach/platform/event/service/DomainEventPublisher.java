package com.outreach.platform.event.service;

import com.outreach.platform.event.config.MongoDbInitializer;
import com.outreach.platform.event.model.DomainEvent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Writes domain events to the MongoDB outbox collection for reliable inter-service messaging.
 */
@Service
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final MongoTemplate mongoTemplate;

    @Inject
    public DomainEventPublisher(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Publishes a domain event to the MongoDB outbox collection.
     *
     * @param eventType the type of domain event (e.g., "EventStatusChanged")
     * @param payload   the event payload as key-value pairs
     */
    public void publish(String eventType, Map<String, Object> payload) {
        DomainEvent domainEvent = new DomainEvent(eventType, payload);
        mongoTemplate.save(domainEvent, MongoDbInitializer.COLLECTION_DOMAIN_EVENTS);
        log.info("Published domain event: type={}, id={}", eventType, domainEvent.getId());
    }
}
