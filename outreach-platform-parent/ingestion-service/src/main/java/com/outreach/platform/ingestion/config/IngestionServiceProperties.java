package com.outreach.platform.ingestion.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/** Externalized configuration properties for the Ingestion Service. */
@Validated
@ConfigurationProperties(prefix = "ingestion-service")
public record IngestionServiceProperties(
        @NotBlank
        String inputDirectory,

        int maxFileSizeMb,

        List<String> allowedExtensions,

        int maxConcurrentJobs
) {
    public IngestionServiceProperties {
        if (maxFileSizeMb <= 0) {
            maxFileSizeMb = 25;
        }
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            allowedExtensions = List.of(".xlsx", ".xls", ".csv");
        }
        if (maxConcurrentJobs <= 0) {
            maxConcurrentJobs = 5;
        }
    }
}
