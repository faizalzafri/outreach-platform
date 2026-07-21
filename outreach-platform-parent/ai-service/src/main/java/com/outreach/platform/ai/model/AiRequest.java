package com.outreach.platform.ai.model;

/**
 * Request payload for AI service operations.
 *
 * @param eventId  the event identifier for context
 * @param prompt   the user-provided prompt or data for AI processing
 */
public record AiRequest(
        String eventId,
        String prompt
) {}
