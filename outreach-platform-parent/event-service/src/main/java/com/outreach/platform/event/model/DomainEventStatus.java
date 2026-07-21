package com.outreach.platform.event.model;

/**
 * Status values for domain events in the outbox collection.
 */
public enum DomainEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
