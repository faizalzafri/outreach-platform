package com.outreach.platform.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Response DTO for AI service health and capabilities status.
 */
@Schema(description = "AI service health and capabilities status")
public record AiStatusResponse(
        @Schema(description = "Configured AI provider name", example = "openai")
        String provider,
        @Schema(description = "Map of feature names to their enabled/disabled status")
        Map<String, Boolean> features,
        @Schema(description = "Overall health status", example = "UP")
        String health
) {}
