package com.outreach.platform.feedback.model.dto;

import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Query parameters DTO for feedback search endpoint.
 */
public record FeedbackSearchRequest(
        UUID eventId,
        UUID employeeId,
        String category,
        FeedbackSentiment sentiment,
        FeedbackStatus status,
        Integer minScore,
        Integer maxScore,
        Instant dateFrom,
        Instant dateTo
) {
}
