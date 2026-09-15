package com.outreach.platform.common.integration;

import com.outreach.platform.common.tenant.TenantFilterAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Minimal Spring Boot test application for integration testing of common-lib components.
 * Excludes JPA and management security auto-configuration since we don't need
 * a real database or full security chain for these tests.
 * Also excludes TenantFilterAspect which requires EntityManager (JPA infrastructure).
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class
})
@ComponentScan(
        basePackages = "com.outreach.platform.common",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TenantFilterAspect.class
        )
)
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
