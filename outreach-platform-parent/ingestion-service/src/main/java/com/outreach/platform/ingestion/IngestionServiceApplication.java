package com.outreach.platform.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for the Ingestion Service.
 * Handles Excel/CSV file upload, parsing, validation, and job tracking.
 */
@SpringBootApplication(scanBasePackages = "com.outreach.platform")
@EnableFeignClients
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
public class IngestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionServiceApplication.class, args);
    }
}
