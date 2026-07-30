package com.outreach.platform.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Registers tenant-related beans for the auth-service. */
@Configuration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantConfig {

    /** System clock bean; tests can override with a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
