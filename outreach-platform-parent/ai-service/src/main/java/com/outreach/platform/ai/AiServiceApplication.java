package com.outreach.platform.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main application entry point for the AI Service.
 * Provides AI-powered features: feedback summarization, anomaly detection, and natural language queries.
 * All endpoints are restricted to ROLE_ADMIN only.
 * Uses MongoDB for storing AI job results with configurable TTL.
 */
@SpringBootApplication(scanBasePackages = "com.outreach.platform")
@ConfigurationPropertiesScan
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
