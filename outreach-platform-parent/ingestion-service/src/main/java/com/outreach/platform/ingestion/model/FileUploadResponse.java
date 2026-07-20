package com.outreach.platform.ingestion.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Response returned after a file upload is accepted for processing.
 *
 * @param jobId     unique job identifier
 * @param fileName  original file name
 * @param status    acceptance status (always "ACCEPTED" on success)
 * @param timestamp time the upload was accepted
 */
public record FileUploadResponse(
        UUID jobId,
        String fileName,
        String status,
        Instant timestamp
) {
}
