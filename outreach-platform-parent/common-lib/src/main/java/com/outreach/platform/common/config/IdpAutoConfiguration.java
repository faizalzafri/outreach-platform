package com.outreach.platform.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration that registers {@link IdpProperties} as a Spring bean
 * in any service that includes common-lib on the classpath.
 *
 * <p>Services can then inject {@code IdpProperties} to access the active
 * identity provider settings ({@code idp.provider} and {@code idp.jwks-uri}).
 */
@Configuration
@EnableConfigurationProperties(IdpProperties.class)
public class IdpAutoConfiguration {
}
