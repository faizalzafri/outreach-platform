package com.outreach.platform.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload from AI service operations.
 */
@Schema(description = "Response payload from an AI service operation")
public record AiResponse(
        @Schema(description = "AI-generated content (summary, analysis, or query result)")
        String content,
        @Schema(description = "AI model used for generation", example = "gpt-4o")
        String model,
        @Schema(description = "AI provider that served the request", example = "openai")
        String provider
) {}
