package com.outreach.platform.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuration class that registers tenant-related beans for the auth-service.
 *
 * <p>This configuration is always active (not conditional on identity provider), because
 * tenant grace period logic applies regardless of whether the embedded Spring Authorization
 * Server or Keycloak is used as the identity provider.
 */
@Configuration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantConfig {

    /**
     * Provides a system-default {@link Clock} bean for date/time operations.
     * Using a bean allows tests to inject a fixed clock for deterministic behavior.
     *
     * @return the system default clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
