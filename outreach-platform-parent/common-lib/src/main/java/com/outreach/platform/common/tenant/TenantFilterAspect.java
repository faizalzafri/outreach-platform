package com.outreach.platform.common.tenant;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * AOP aspect that enables the Hibernate tenant filter around JPA repository method execution.
 *
 * <p>A Hibernate filter lives on one session, so it must be enabled on the session the query will
 * run in. Inside a transaction that is the transaction's session. Outside one, enabling it on the
 * shared EntityManager would touch a throwaway session and the query would run unfiltered, so the
 * call is wrapped in a transaction first; the repository's own transaction joins it.
 *
 * <p>Deliberately NOT {@code @Named}/{@code @Component} — every service's
 * {@code @SpringBootApplication(scanBasePackages = "com.outreach.platform")} is broad enough to
 * component-scan common-lib directly, which would instantiate this unconditionally and bypass
 * {@code TenantAutoConfiguration.TenantJpaAutoConfiguration}'s guard — breaking any context that
 * legitimately has no JPA (e.g. ingestion-service's Mongo-only Testcontainers IT tests). Only
 * reachable via that {@code @Bean} method.
 */
@Aspect
public class TenantFilterAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantFilterAspect.class);

    private static final String PLATFORM_ADMIN_AUTHORITY = "ROLE_PLATFORM_ADMIN";

    private final Supplier<EntityManager> entityManager;
    private final Supplier<TransactionTemplate> transaction;

    public TenantFilterAspect(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this(() -> entityManager, () -> transactionManager);
    }

    /**
     * Resolves JPA lazily, on the first repository call: the aspect is registered wherever JPA is on
     * the classpath, including contexts that never create a repository.
     */
    public TenantFilterAspect(Supplier<EntityManager> entityManager, Supplier<PlatformTransactionManager> transactionManager) {
        this.entityManager = memoize(entityManager);
        // Not read-only: the wrapped call may be a save or delete.
        this.transaction = memoize(() -> new TransactionTemplate(transactionManager.get()));
    }

    private static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return new Supplier<>() {
            private volatile T value;

            @Override
            public T get() {
                if (value == null) {
                    value = supplier.get();
                }
                return value;
            }
        };
    }

    @Pointcut("execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..))")
    public void jpaRepositoryMethods() {
    }

    /**
     * Runs the repository call with the tenant filter on. Platform admins are not filtered, so
     * they can work across tenants; anyone else without a tenant is refused.
     */
    @Around("jpaRepositoryMethods()")
    public Object withTenantFilter(ProceedingJoinPoint call) throws Throwable {
        if (isPlatformAdmin()) {
            log.debug("Platform admin detected — skipping tenant filter enablement");
            return call.proceed();
        }
        if (!TenantContext.isPresent()) {
            throw new IllegalStateException("Tenant filter expected but TenantContext is empty");
        }
        UUID tenantId = TenantContext.getCurrentTenantId();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            enableFilter(tenantId);
            return call.proceed();
        }
        try {
            return transaction.get().execute(status -> {
                enableFilter(tenantId);
                try {
                    return call.proceed();
                } catch (Throwable t) {
                    throw new CallFailure(t);
                }
            });
        } catch (CallFailure failure) {
            throw failure.getCause();
        }
    }

    private void enableFilter(UUID tenantId) {
        entityManager.get().unwrap(Session.class)
                .enableFilter(TenantConstants.TENANT_FILTER_NAME)
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

    /** Carries the repository's own exception out of the transaction callback unchanged. */
    private static final class CallFailure extends RuntimeException {
        CallFailure(Throwable cause) {
            super(cause);
        }
    }
}
