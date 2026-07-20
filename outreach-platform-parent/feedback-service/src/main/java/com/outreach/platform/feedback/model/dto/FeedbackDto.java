package com.outreach.platform.feedback.model.dto;

import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a volunteer feedback record.
 */
public record FeedbackDto(
        UUID id,
        UUID eventId,
        UUID volunteerId,
        int score,
        String answer1,
        String answer2,
        String answer3,
        String category,
        String tags,
        FeedbackSentiment sentiment,
        FeedbackStatus status,
        boolean anonymous,
        Instant submittedAt,
        Instant reviewedAt,
        String reviewedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
