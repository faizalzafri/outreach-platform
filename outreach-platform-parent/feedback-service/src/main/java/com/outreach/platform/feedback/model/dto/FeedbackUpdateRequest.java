package com.outreach.platform.feedback.model.dto;

import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for updating an existing feedback record.
 */
@Schema(description = "Request payload for updating existing feedback. All fields are optional.")
public record FeedbackUpdateRequest(
        @Schema(description = "Updated score (1-5)", example = "5", minimum = "1", maximum = "5")
        @Min(1) @Max(5) Integer score,
        @Schema(description = "Updated first answer", example = "The event was excellent")
        String answer1,
        @Schema(description = "Updated second answer", example = "Great teamwork")
        String answer2,
        @Schema(description = "Updated third answer", example = "Highly recommend")
        String answer3,
        @Schema(description = "Updated category", example = "volunteer-experience")
        String category,
        @Schema(description = "Updated tags", example = "excellent,teamwork")
        String tags,
        @Schema(description = "Updated sentiment", example = "POSITIVE")
        FeedbackSentiment sentiment,
        @Schema(description = "Updated status", example = "REVIEWED")
        FeedbackStatus status
) {
}
