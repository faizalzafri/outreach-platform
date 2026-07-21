package com.outreach.platform.ingestion.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response returned after a file upload is accepted for processing.
 */
@Schema(description = "Response returned after a file upload is accepted for processing")
public record FileUploadResponse(
        @Schema(description = "Unique job identifier for tracking", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID jobId,
        @Schema(description = "Original file name", example = "volunteers-march-2024.xlsx")
        String fileName,
        @Schema(description = "Acceptance status", example = "ACCEPTED")
        String status,
        @Schema(description = "Timestamp when the upload was accepted")
        Instant timestamp
) {
}
