package com.outreach.platform.ingestion.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MongoDB document representing a domain event in the outbox pattern.
 */
// Own collection, not event-service's "domain_events": every service shares one Mongo database, and
// each outbox poller publishes every PENDING document in its collection. Sharing one made both
// pollers publish each other's events (duplicate messages) and crashed this poller on event-service
// documents, whose payload is a JSON string rather than a map.
@Document("ingestion_domain_events")
public class DomainEventDocument {

    @Id
    private String id;
    private String eventType;
    private Map<String, Object> payload;
    private EventStatus status;

    @Indexed
    private UUID tenantId;

    private Instant createdAt;
    private Instant publishedAt;

    public DomainEventDocument() {
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

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
