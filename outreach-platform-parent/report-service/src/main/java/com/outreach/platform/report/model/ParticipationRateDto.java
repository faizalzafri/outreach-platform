package com.outreach.platform.report.model;

import java.math.BigDecimal;

/**
 * Volunteer participation rate statistics.
 */
public record ParticipationRateDto(
        long totalRegistered,
        long totalAttended,
        BigDecimal participationRate,
        BigDecimal feedbackSubmissionRate
) {
}
