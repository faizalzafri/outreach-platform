package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * Common query parameters shared across all report aggregation endpoints.
 */
@Schema(description = "Common query parameters for report aggregation endpoints")
public record ReportQueryParams(
        @Schema(description = "Start of date range", example = "2024-01-01")
        LocalDate dateFrom,
        @Schema(description = "End of date range", example = "2024-12-31")
        LocalDate dateTo,
        @Schema(description = "Filter by event IDs")
        List<String> eventIds,
        @Schema(description = "Filter by cities")
        List<String> cities,
        @Schema(description = "Filter by beneficiary IDs")
        List<String> beneficiaryIds,
        @Schema(description = "Filter by POC IDs")
        List<String> pocIds,
        @Schema(description = "Time granularity for aggregation", example = "month")
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
