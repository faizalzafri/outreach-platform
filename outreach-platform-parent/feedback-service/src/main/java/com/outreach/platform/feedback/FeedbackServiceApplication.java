package com.outreach.platform.feedback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application entry point for the Feedback Service.
 * Responsible for accepting, validating, and persisting volunteer feedback submissions.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.outreach.platform.feedback", "com.outreach.platform.common"})
@EnableJpaAuditing
@EnableFeignClients
@EnableCaching
@ConfigurationPropertiesScan
public class FeedbackServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedbackServiceApplication.class, args);
    }
}
