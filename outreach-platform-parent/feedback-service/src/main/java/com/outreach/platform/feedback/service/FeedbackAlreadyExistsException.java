package com.outreach.platform.feedback.service;

import java.util.UUID;

/**
 * Thrown when attempting to submit feedback that already exists for the composite key.
 */
public class FeedbackAlreadyExistsException extends RuntimeException {

    public FeedbackAlreadyExistsException(UUID eventId, UUID employeeId) {
        super("Feedback already exists for eventId=%s and employeeId=%s".formatted(eventId, employeeId));
    }
}
