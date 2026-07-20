package com.outreach.platform.feedback.model.dto;

import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for updating an existing feedback record.
 */
public record FeedbackUpdateRequest(
        @Min(1) @Max(5) Integer score,
        String answer1,
        String answer2,
        String answer3,
        String category,
        String tags,
        FeedbackSentiment sentiment,
        FeedbackStatus status
) {
}
