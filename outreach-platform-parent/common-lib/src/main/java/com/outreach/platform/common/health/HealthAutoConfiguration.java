package com.outreach.platform.common.health;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration that registers custom health indicators and the health status transition logger when Actuator is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(org.springframework.boot.actuate.health.HealthEndpoint.class)
@Import({SmtpHealthIndicator.class, IdpHealthIndicator.class, HealthStatusTransitionLogger.class})
@EnableScheduling
public class HealthAutoConfiguration {
}
