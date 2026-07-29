package com.outreach.platform.event.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * MongoDB document representing a domain event in the outbox.
 * Stores events as PENDING until the outbox poller publishes them.
 */
@Document(collection = "domain_events")
public class DomainEventDocument {

    @Id
    private String id;
    private String eventType;
    private String payload;
    private DomainEventStatus status;

    @Indexed
    private UUID tenantId;

    private Instant createdAt;
    private Instant publishedAt;
    private int retryCount;

    public DomainEventDocument() {
    }

    public DomainEventDocument(String eventType, String payload) {
        this.id = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.payload = payload;
        this.status = DomainEventStatus.PENDING;
        this.createdAt = Instant.now();
        this.retryCount = 0;
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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public DomainEventStatus getStatus() {
        return status;
    }

    public void setStatus(DomainEventStatus status) {
        this.status = status;
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

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
