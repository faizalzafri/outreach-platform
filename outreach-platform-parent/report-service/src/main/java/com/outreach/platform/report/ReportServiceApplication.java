package com.outreach.platform.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application entry point for the Report Service.
 *
 * <p>Provides read-only analytics aggregation, dashboard data, and report
 * generation. This service does not write to PostgreSQL and therefore does
 * not enable JPA auditing or Liquibase migrations.</p>
 */
@SpringBootApplication(scanBasePackages = "com.outreach.platform")
@EnableFeignClients
@EnableCaching
@ConfigurationPropertiesScan
public class ReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}
