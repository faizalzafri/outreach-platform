package com.outreach.platform.ingestion.client;

/** Thrown when the Event Service is unavailable during ingestion operations. */
public class EventServiceUnavailableException extends RuntimeException {

    public EventServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
