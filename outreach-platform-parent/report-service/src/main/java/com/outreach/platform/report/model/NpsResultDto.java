package com.outreach.platform.report.model;

import java.math.BigDecimal;

/**
 * Net Promoter Score breakdown by event.
 * NPS is calculated from feedback scores: promoters (4-5), passives (3), detractors (1-2).
 */
public record NpsResultDto(
        String eventId,
        String eventName,
        long promoters,
        long passives,
        long detractors,
        BigDecimal npsScore,
        long totalResponses
) {
}
