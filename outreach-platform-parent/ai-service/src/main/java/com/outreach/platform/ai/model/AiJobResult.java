package com.outreach.platform.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Internal result holder returned by AiService operations.
 */
@Schema(description = "Internal result holder from AI service operations")
public record AiJobResult(
        @Schema(description = "AI-generated content")
        String content,
        @Schema(description = "Whether the operation succeeded", example = "true")
        boolean success,
        @Schema(description = "Error details if the operation failed")
        String errorMessage
) {
    public static AiJobResult success(String content) {
        return new AiJobResult(content, true, null);
    }

    public static AiJobResult failure(String errorMessage) {
        return new AiJobResult(null, false, errorMessage);
    }
}
