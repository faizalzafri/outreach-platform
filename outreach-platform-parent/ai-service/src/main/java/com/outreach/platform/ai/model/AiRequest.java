package com.outreach.platform.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload for AI service operations.
 */
@Schema(description = "Generic request payload for AI service operations")
public record AiRequest(
        @Schema(description = "Event identifier for context", example = "EVT-2024-001")
        String eventId,
        @Schema(description = "User-provided prompt or data for AI processing", example = "Summarize feedback for this event")
        String prompt
) {}
