package com.outreach.platform.feedback.service;

import java.util.UUID;

/**
 * Thrown when a feedback record is not found for the given composite key.
 */
public class FeedbackNotFoundException extends RuntimeException {

    public FeedbackNotFoundException(UUID eventId, UUID employeeId) {
        super("Feedback not found for eventId=%s and employeeId=%s".formatted(eventId, employeeId));
    }
}
