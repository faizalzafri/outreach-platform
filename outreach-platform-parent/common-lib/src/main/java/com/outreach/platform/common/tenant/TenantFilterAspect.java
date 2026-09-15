package com.outreach.platform.common.tenant;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.UUID;

/**
 * AOP aspect that enables the Hibernate tenant filter before JPA repository method execution.
 *
 * <p>Deliberately NOT {@code @Named}/{@code @Component} — every service's
 * {@code @SpringBootApplication(scanBasePackages = "com.outreach.platform")} is broad enough to
 * component-scan common-lib directly, which would instantiate this unconditionally and bypass
 * {@code TenantAutoConfiguration.TenantJpaAutoConfiguration}'s {@code @ConditionalOnBean(EntityManager.class)}
 * guard — breaking any context that legitimately has no {@code EntityManager} (e.g.
 * ingestion-service's Mongo-only Testcontainers IT tests, which exclude JPA autoconfiguration
 * entirely). Confirmed by that exact failure: a bare component-scanned instance failed to
 * construct with "No qualifying bean of type EntityManager available" even though the guarded
 * factory method should have skipped it. Only reachable via that {@code @Bean} method now.
 */
@Aspect
public class TenantFilterAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantFilterAspect.class);

    private static final String PLATFORM_ADMIN_AUTHORITY = "ROLE_PLATFORM_ADMIN";

    private final EntityManager entityManager;

    @Inject
    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Pointcut("execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..))")
    public void jpaRepositoryMethods() {
    }

    /**
     * Enables the Hibernate tenant filter before repository method execution.
     * Skips enablement for platform admins to allow cross-tenant access.
     */
    @Before("jpaRepositoryMethods()")
    public void enableTenantFilter() {
        if (isPlatformAdmin()) {
            log.debug("Platform admin detected — skipping tenant filter enablement");
            return;
        }

        if (!TenantContext.isPresent()) {
            throw new IllegalStateException(
                    "Tenant filter expected but TenantContext is empty"
            );
        }

        UUID tenantId = TenantContext.getCurrentTenantId();

        Session session = entityManager.unwrap(Session.class);
        session.enableFilter(TenantConstants.TENANT_FILTER_NAME)
                .setParameter("tenantId", tenantId);

        log.debug("Tenant filter enabled with tenantId: {}", tenantId);
    }

    private boolean isPlatformAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            return false;
        }

        return authorities.stream()
                .anyMatch(authority -> PLATFORM_ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }
}
