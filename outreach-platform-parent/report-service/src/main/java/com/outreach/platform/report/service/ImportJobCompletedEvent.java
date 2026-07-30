package com.outreach.platform.report.service;

/** Event published when an import job completes, triggering cache invalidation for analytics data. */
public record ImportJobCompletedEvent(
        String jobId
) {
}
