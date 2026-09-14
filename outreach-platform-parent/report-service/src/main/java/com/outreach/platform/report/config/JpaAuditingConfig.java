package com.outreach.platform.report.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA auditing configuration, kept as a standalone class (not on the main
 * {@code @SpringBootApplication} class) so {@code @WebMvcTest} slices don't try to initialize it —
 * they only include the root configuration class, and a separate {@code @Configuration} class is
 * correctly excluded from a web-layer slice's component scan.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {
}
