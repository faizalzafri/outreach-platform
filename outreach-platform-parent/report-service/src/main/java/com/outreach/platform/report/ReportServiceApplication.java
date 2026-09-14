package com.outreach.platform.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Read-only analytics aggregation, dashboard data, and report generation service.
 *
 * <p>{@code @EnableJpaAuditing} lives in a separate {@code JpaAuditingConfig} class (see
 * {@code config/JpaAuditingConfig.java}), not here — putting it directly on this
 * {@code @SpringBootApplication} class breaks {@code @WebMvcTest} slices with "JPA metamodel
 * must not be empty", since the root configuration class is always included in a web slice but
 * standalone {@code @Configuration} classes are correctly excluded from it.
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
