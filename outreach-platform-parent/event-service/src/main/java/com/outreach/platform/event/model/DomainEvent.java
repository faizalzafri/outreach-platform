package com.outreach.platform.event.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event document stored in the MongoDB outbox collection.
 * Used for reliable inter-service event publishing via the transactional outbox pattern.
 */
public class DomainEvent {

    private String id;
    private String eventType;
    private Map<String, Object> payload;
    private String status;
    private Instant createdAt;
    private Instant processedAt;

    public DomainEvent() {
    }

    public DomainEvent(String eventType, Map<String, Object> payload) {
        this.id = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.payload = payload;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
