package com.outreach.platform.common.tenant;

import java.util.UUID;

/**
 * Thread-local holder for the current tenant identity.
 * <p>
 * Each incoming request is associated with exactly one tenant. The tenant ID is extracted
 * from the {@code X-Tenant-ID} header by {@code TenantContextFilter} and stored here
 * for the duration of the request. All downstream components (Hibernate filters, entity
 * listeners, cache key generators, message post-processors) read from this context.
 * <p>
 * <strong>Important:</strong> Always call {@link #clear()} in a {@code finally} block
 * to prevent thread-local leakage in pooled thread environments.
 *
 * @see TenantConstants#X_TENANT_ID_HEADER
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // utility class — not instantiable
    }

    /**
     * Returns the tenant ID bound to the current thread.
     *
     * @return the current tenant UUID, or {@code null} if no tenant context is set
     */
    public static UUID getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Binds the given tenant ID to the current thread.
     *
     * @param tenantId the tenant UUID to associate with this thread; must not be {@code null}
     * @throws IllegalArgumentException if {@code tenantId} is {@code null}
     */
    public static void setCurrentTenantId(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Removes the tenant context from the current thread.
     * This must be called in a {@code finally} block after request processing completes.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /**
     * Checks whether a tenant context is currently set on this thread.
     *
     * @return {@code true} if a tenant ID is present, {@code false} otherwise
     */
    public static boolean isPresent() {
        return CURRENT_TENANT.get() != null;
    }
}
