package com.outreach.platform.feedback.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for submitting new volunteer feedback.
 */
public record FeedbackSubmitRequest(
        @NotNull UUID eventId,
        @NotNull UUID volunteerId,
        @Min(1) @Max(5) int score,
        String answer1,
        String answer2,
        String answer3,
        String category,
        String tags,
        boolean anonymous
) {
}
