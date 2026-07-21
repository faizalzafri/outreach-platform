package com.outreach.platform.common.health;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for custom health indicators and health status monitoring.
 *
 * <p>Registers:
 * <ul>
 *   <li>{@link SmtpHealthIndicator} — active only when {@code spring.mail.host} is set</li>
 *   <li>{@link IdpHealthIndicator} — active only when JWKS URI is configured</li>
 *   <li>{@link HealthStatusTransitionLogger} — polls health status and logs transitions</li>
 * </ul>
 *
 * <p>This configuration is activated automatically via Spring Boot's auto-configuration
 * mechanism when the actuator health endpoint is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(org.springframework.boot.actuate.health.HealthEndpoint.class)
@Import({SmtpHealthIndicator.class, IdpHealthIndicator.class, HealthStatusTransitionLogger.class})
@EnableScheduling
public class HealthAutoConfiguration {
}
