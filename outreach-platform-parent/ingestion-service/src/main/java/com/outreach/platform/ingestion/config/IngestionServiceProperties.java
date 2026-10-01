package com.outreach.platform.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Externalized configuration properties for the Ingestion Service. */
@ConfigurationProperties(prefix = "ingestion-service")
public record IngestionServiceProperties(
        List<String> allowedExtensions
) {
    public IngestionServiceProperties {
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            allowedExtensions = List.of(".xlsx", ".xls", ".csv");
        }
    }
}
