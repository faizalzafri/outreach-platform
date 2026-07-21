package com.outreach.platform.report.service;

/**
 * Application event indicating that an import job has completed.
 * Published locally when an inter-service ImportJobCompleted message is received,
 * triggering cache invalidation for analytics data.
 */
public record ImportJobCompletedEvent(
        String jobId
) {
}
