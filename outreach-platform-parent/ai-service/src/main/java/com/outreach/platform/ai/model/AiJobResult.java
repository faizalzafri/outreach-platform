package com.outreach.platform.ai.model;

/**
 * Internal result holder returned by AiService operations.
 *
 * @param content the AI-generated content
 * @param success whether the operation succeeded
 * @param errorMessage error details if the operation failed
 */
public record AiJobResult(
        String content,
        boolean success,
        String errorMessage
) {
    public static AiJobResult success(String content) {
        return new AiJobResult(content, true, null);
    }

    public static AiJobResult failure(String errorMessage) {
        return new AiJobResult(null, false, errorMessage);
    }
}
