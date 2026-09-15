package com.outreach.platform.common.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

/**
 * Registers {@link SecurityAuditorAware} as the {@code "auditorAware"} bean for every service,
 * regardless of that service's own {@code scanBasePackages} — so
 * {@code @EnableJpaAuditing(auditorAwareRef = "auditorAware")} resolves consistently everywhere
 * instead of only in services whose component scan happens to sweep up this package.
 *
 * <p>{@code @ConditionalOnMissingBean(name = "auditorAware")} lets a service that defines its own
 * bean under that name (auth-service does) keep using it instead.
 */
@AutoConfiguration
@ConditionalOnClass(AuditorAware.class)
public class AuditingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "auditorAware")
    public AuditorAware<String> auditorAware() {
        return new SecurityAuditorAware();
    }
}
