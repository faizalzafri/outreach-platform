package com.outreach.platform.ai.model;

import java.util.Map;

/**
 * Response DTO for AI service health and capabilities status.
 *
 * @param provider   the configured AI provider name
 * @param features   map of feature names to their enabled/disabled status
 * @param health     overall health status of the AI service
 */
public record AiStatusResponse(
        String provider,
        Map<String, Boolean> features,
        String health
) {}
