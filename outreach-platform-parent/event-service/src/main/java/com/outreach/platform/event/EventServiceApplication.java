package com.outreach.platform.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.outreach.platform.event.config.EventServiceProperties;

/**
 * Main application entry point for the Event Service.
 *
 * <p>Enables JPA auditing (powered by common-lib's {@code SecurityAuditorAware} bean
 * registered as "auditorAware"), declarative OpenFeign clients for inter-service
 * communication, and type-safe configuration properties.</p>
 */
@SpringBootApplication(scanBasePackages = "com.outreach.platform")
@EnableJpaAuditing(auditorRef = "auditorAware")
@EnableFeignClients
@EnableConfigurationProperties(EventServiceProperties.class)
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}
