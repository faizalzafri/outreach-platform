package com.outreach.platform.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for natural language queries against platform data.
 */
@Schema(description = "Request payload for natural language queries against platform data")
public record QueryRequest(
        @Schema(description = "Natural language question", example = "What is the average feedback score for events in Bangalore?", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 2000) String prompt,
        @Schema(description = "Optional additional context to scope the query", example = "Focus on Q1 2024 events")
        String context
) {}
