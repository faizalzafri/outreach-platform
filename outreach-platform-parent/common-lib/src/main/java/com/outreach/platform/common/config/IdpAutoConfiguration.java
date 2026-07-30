package com.outreach.platform.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers IdpProperties for any service that includes common-lib on the classpath.
 */
@Configuration
@EnableConfigurationProperties(IdpProperties.class)
public class IdpAutoConfiguration {
}
