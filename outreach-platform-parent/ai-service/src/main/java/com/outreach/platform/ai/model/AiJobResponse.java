package com.outreach.platform.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO for AI job status and results.
 */
@Schema(description = "AI job status and result response")
public record AiJobResponse(
        @Schema(description = "Unique job identifier", example = "6614a1b2c3d4e5f6a7b8c9d0")
        String jobId,
        @Schema(description = "Current job status", example = "COMPLETED")
        String status,
        @Schema(description = "Job result content (null if not yet completed)")
        String result,
        @Schema(description = "Job creation timestamp")
        Instant createdAt
) {}
