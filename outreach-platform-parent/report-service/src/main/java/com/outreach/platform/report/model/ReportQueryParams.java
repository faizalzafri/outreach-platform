package com.outreach.platform.report.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Common query parameters shared across all report aggregation endpoints.
 */
public record ReportQueryParams(
        LocalDate dateFrom,
        LocalDate dateTo,
        List<String> eventIds,
        List<String> cities,
        List<String> beneficiaryIds,
        List<String> pocIds,
        String granularity
) {

    /**
     * Returns the granularity or defaults to "month" if not specified.
     */
    public String effectiveGranularity() {
        if (granularity == null || granularity.isBlank()) {
            return "month";
        }
        return granularity.toLowerCase();
    }
}
