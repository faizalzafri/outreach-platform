package com.outreach.platform.common.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * AuditorAware implementation that extracts the current user from the SecurityContext for @CreatedBy/@LastModifiedBy fields.
 *
 * <p>Registered as the {@code "auditorAware"} bean by {@link AuditingAutoConfiguration} rather
 * than via {@code @Component} directly — a bare component here would only be picked up by
 * services broad enough to component-scan {@code com.outreach.platform.common} (which is
 * incidental to their own {@code scanBasePackages}, not something every service does), so
 * several services never got this bean at all and silently persisted null
 * {@code created_by}/{@code updated_by} values.
 */
public class SecurityAuditorAware implements AuditorAware<String> {

    private static final String SYSTEM_USER = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of(SYSTEM_USER);
        }

        String principal = authentication.getName();
        if (principal == null || principal.isBlank() || "anonymousUser".equals(principal)) {
            return Optional.of(SYSTEM_USER);
        }

        return Optional.of(principal);
    }
}
