package com.outreach.platform.ingestion.client;

/**
 * Exception thrown when the Event Service is unavailable during ingestion operations.
 * Callers should catch this to fail the import job gracefully or schedule a retry.
 */
public class EventServiceUnavailableException extends RuntimeException {

    public EventServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
