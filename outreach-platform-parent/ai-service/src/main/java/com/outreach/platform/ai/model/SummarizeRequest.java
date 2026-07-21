package com.outreach.platform.ai.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for feedback summarization.
 *
 * @param context the text content to summarize (e.g., feedback data for an event)
 * @param maxLength optional max length hint for the summary
 */
public record SummarizeRequest(
        @NotBlank @Size(max = 50000) String context,
        Integer maxLength
) {}
