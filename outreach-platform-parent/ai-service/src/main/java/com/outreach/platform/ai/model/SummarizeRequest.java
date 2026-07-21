package com.outreach.platform.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for feedback summarization.
 */
@Schema(description = "Request payload for AI feedback summarization")
public record SummarizeRequest(
        @Schema(description = "Text content to summarize (e.g., feedback data for an event)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 50000) String context,
        @Schema(description = "Maximum length hint for the summary", example = "500")
        Integer maxLength
) {}
