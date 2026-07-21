package com.outreach.platform.feedback.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for submitting new volunteer feedback.
 */
@Schema(description = "Request payload for submitting new volunteer feedback")
public record FeedbackSubmitRequest(
        @Schema(description = "Event ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID eventId,
        @Schema(description = "Volunteer ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID volunteerId,
        @Schema(description = "Score from 1 (poor) to 5 (excellent)", example = "4", minimum = "1", maximum = "5")
        @Min(1) @Max(5) int score,
        @Schema(description = "First survey answer", example = "The event was well organized")
        String answer1,
        @Schema(description = "Second survey answer", example = "Great volunteer coordination")
        String answer2,
        @Schema(description = "Third survey answer", example = "Would participate again")
        String answer3,
        @Schema(description = "Feedback category", example = "event-quality")
        String category,
        @Schema(description = "Comma-separated tags", example = "positive,organized")
        String tags,
        @Schema(description = "Whether to submit anonymously", example = "false")
        boolean anonymous
) {
}
