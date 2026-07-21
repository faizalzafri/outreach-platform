package com.outreach.platform.common.messaging;

import java.time.Instant;
import java.util.Map;

/**
 * Standard domain event message format for RabbitMQ transport.
 * All services publish and consume events using this record structure.
 */
public record DomainEventMessage(
        String eventId,
        String eventType,
        Map<String, Object> payload,
        Instant timestamp
) {
    public DomainEventMessage {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public DomainEventMessage(String eventId, String eventType, Map<String, Object> payload) {
        this(eventId, eventType, payload, Instant.now());
    }
}
