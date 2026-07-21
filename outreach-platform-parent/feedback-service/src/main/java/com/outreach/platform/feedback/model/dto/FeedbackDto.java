package com.outreach.platform.feedback.model.dto;

import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a volunteer feedback record.
 */
@Schema(description = "Read-only representation of a volunteer feedback record")
public record FeedbackDto(
        @Schema(description = "Feedback unique identifier")
        UUID id,
        @Schema(description = "Event ID this feedback belongs to")
        UUID eventId,
        @Schema(description = "Volunteer ID who submitted the feedback")
        UUID volunteerId,
        @Schema(description = "Feedback score (1-5)", example = "4")
        int score,
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
        @Schema(description = "AI-detected sentiment", example = "POSITIVE")
        FeedbackSentiment sentiment,
        @Schema(description = "Current feedback status", example = "SUBMITTED")
        FeedbackStatus status,
        @Schema(description = "Whether feedback is anonymous", example = "false")
        boolean anonymous,
        @Schema(description = "Timestamp when feedback was submitted")
        Instant submittedAt,
        @Schema(description = "Timestamp when feedback was reviewed")
        Instant reviewedAt,
        @Schema(description = "Username of the reviewer", example = "pmo_user")
        String reviewedBy,
        @Schema(description = "Creation timestamp")
        Instant createdAt,
        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {
}
