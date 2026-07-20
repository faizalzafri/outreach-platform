package com.outreach.platform.feedback.model.dto;

import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Search criteria for querying feedback records with multiple filters.
 */
public record FeedbackSearchCriteria(
        UUID eventId,
        UUID volunteerId,
        String category,
        FeedbackSentiment sentiment,
        FeedbackStatus status,
        Integer minScore,
        Integer maxScore,
        Instant dateFrom,
        Instant dateTo
) {
}
