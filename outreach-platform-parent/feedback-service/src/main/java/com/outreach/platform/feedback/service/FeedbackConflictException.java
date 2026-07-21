package com.outreach.platform.feedback.service;

import java.util.UUID;

/**
 * Thrown when an optimistic locking conflict occurs during feedback update.
 */
public class FeedbackConflictException extends RuntimeException {

    public FeedbackConflictException(UUID eventId, UUID employeeId) {
        super("Concurrent modification conflict for eventId=%s and employeeId=%s".formatted(eventId, employeeId));
    }
}
