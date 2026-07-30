package com.outreach.platform.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Read-only analytics aggregation, dashboard data, and report generation service. */
@SpringBootApplication(scanBasePackages = "com.outreach.platform")
@EnableFeignClients
@EnableCaching
@ConfigurationPropertiesScan
public class ReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}
