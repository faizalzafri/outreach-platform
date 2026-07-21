package com.outreach.platform.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application entry point for the Ingestion Service.
 * Handles Excel/CSV file upload, parsing, validation, and job tracking.
 */
@SpringBootApplication(scanBasePackages = "com.outreach.platform")
@EnableFeignClients
@ConfigurationPropertiesScan
public class IngestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionServiceApplication.class, args);
    }
}
