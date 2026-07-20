package com.outreach.platform.ingestion.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Externalized configuration properties for the Ingestion Service.
 *
 * @param inputDirectory     Base directory for file ingestion (externalized, required)
 * @param maxFileSizeMb      Maximum allowed file size in megabytes
 * @param allowedExtensions  List of allowed file extensions (e.g. .xlsx, .xls, .csv)
 * @param maxConcurrentJobs  Maximum number of concurrent ingestion jobs
 */
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
