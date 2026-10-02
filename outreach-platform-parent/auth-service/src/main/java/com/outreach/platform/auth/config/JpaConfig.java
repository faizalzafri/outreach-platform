package com.outreach.platform.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA configuration enabling auditing for entity lifecycle fields
 * (createdDate, lastModifiedDate, createdBy, lastModifiedBy). The "auditorAware" bean comes from
 * common-lib's AuditingAutoConfiguration.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {
}
