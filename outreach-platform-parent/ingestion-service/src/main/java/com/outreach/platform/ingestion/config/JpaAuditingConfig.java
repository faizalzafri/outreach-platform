package com.outreach.platform.ingestion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing configuration, active only outside of integration test profile.
 */
@Configuration
@Profile("!it")
@EnableJpaAuditing
public class JpaAuditingConfig {
}
