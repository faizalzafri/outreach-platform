package com.outreach.platform.common.tenant;

import jakarta.inject.Inject;
import jakarta.inject.Named;
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
 * AOP aspect that automatically enables the Hibernate tenant filter on JPA repository calls.
 * <p>
 * Before any Spring Data JPA repository method executes, this aspect:
 * <ol>
 *   <li>Checks whether the current user holds the {@code ROLE_PLATFORM_ADMIN} authority —
 *       if so, the filter is <em>not</em> enabled, allowing cross-tenant data access.</li>
 *   <li>Reads the tenant ID from {@link TenantContext} and enables the Hibernate
 *       {@code tenantFilter} session filter with that value.</li>
 *   <li>Throws an {@link IllegalStateException} if no tenant context is present and the
 *       user is not a platform admin — this prevents unscoped queries from leaking data
 *       across tenants.</li>
 * </ol>
 * <p>
 * The filter name is read from {@link TenantConstants#TENANT_FILTER_NAME} to keep it
 * consistent with the {@code @FilterDef} declared on {@link TenantAwareBaseEntity}.
 *
 * @see TenantContext
 * @see TenantAwareBaseEntity
 * @see TenantConstants#TENANT_FILTER_NAME
 */
@Aspect
@Named("tenantFilterAspect")
public class TenantFilterAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantFilterAspect.class);

    private static final String PLATFORM_ADMIN_AUTHORITY = "ROLE_PLATFORM_ADMIN";

    private final EntityManager entityManager;

    @Inject
    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Pointcut targeting all methods on Spring Data JPA repository interfaces.
     */
    @Pointcut("execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..))")
    public void jpaRepositoryMethods() {
        // pointcut definition — no body required
    }

    /**
     * Enables the Hibernate tenant filter before repository method execution.
     * <p>
     * Skips enablement for users with {@code ROLE_PLATFORM_ADMIN} authority.
     * Throws {@link IllegalStateException} if tenant context is absent for non-admin users.
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

    /**
     * Checks whether the current authenticated user holds the {@code ROLE_PLATFORM_ADMIN} authority.
     *
     * @return {@code true} if the user is a platform admin, {@code false} otherwise
     */
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
