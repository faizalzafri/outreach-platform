package com.outreach.platform.ai.model;

/**
 * Response payload from AI service operations.
 *
 * @param content  the AI-generated content (summary, analysis, or query result)
 * @param model    the AI model used for the generation
 * @param provider the AI provider that served the request
 */
public record AiResponse(
        String content,
        String model,
        String provider
) {}
