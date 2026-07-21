package com.outreach.platform.ai.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for natural language queries against platform data.
 *
 * @param prompt the natural language question
 * @param context optional additional context to scope the query
 */
public record QueryRequest(
        @NotBlank @Size(max = 2000) String prompt,
        String context
) {}
