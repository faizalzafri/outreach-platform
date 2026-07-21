package com.outreach.platform.ai.model;

import java.time.Instant;

/**
 * Response DTO for AI job status and results.
 *
 * @param jobId   unique identifier of the AI job
 * @param status  current job status (PENDING, RUNNING, COMPLETED, FAILED)
 * @param result  the job result (null if not yet completed)
 * @param createdAt when the job was created
 */
public record AiJobResponse(
        String jobId,
        String status,
        String result,
        Instant createdAt
) {}
