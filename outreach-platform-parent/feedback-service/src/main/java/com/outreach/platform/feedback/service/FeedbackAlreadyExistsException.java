package com.outreach.platform.feedback.service;

/**
 * Thrown when attempting to submit feedback that already exists for the composite key.
 */
public class FeedbackAlreadyExistsException extends RuntimeException {

    public FeedbackAlreadyExistsException() {
        // Shown to the person submitting, so in their terms rather than ids.
        super("This volunteer already has feedback for this event.");
    }
}
