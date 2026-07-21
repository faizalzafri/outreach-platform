package com.outreach.platform.ingestion.model;

/**
 * Status progression for import jobs.
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
