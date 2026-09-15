package com.outreach.platform.report.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Type-safe configuration properties for the Report Service, bound from the "report-service" prefix. */
@ConfigurationProperties(prefix = "report-service")
@Validated
public record ReportServiceProperties(

        @Min(1) @Max(100)
        @DefaultValue("20")
        int defaultPageSize,

        @Min(1) @Max(500)
        @DefaultValue("100")
        int maxPageSize,

        @Min(1) @Max(1440)
        @DefaultValue("10")
        int cacheTtlMinutes,

        @Min(1) @Max(3650)
        @DefaultValue("365")
        int snapshotRetentionDays
) {
}
