package com.outreach.platform.common.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Minimal Spring Boot test application for integration testing of common-lib components.
 * Excludes JPA and management security auto-configuration since we don't need
 * a real database or full security chain for these tests.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class
})
@ComponentScan(basePackages = "com.outreach.platform.common")
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
