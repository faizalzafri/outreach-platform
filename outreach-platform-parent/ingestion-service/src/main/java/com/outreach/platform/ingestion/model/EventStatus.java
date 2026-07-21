package com.outreach.platform.ingestion.model;

/**
 * Status for domain events in the outbox.
 */
public enum EventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
