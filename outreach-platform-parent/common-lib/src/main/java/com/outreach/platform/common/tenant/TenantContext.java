package com.outreach.platform.common.tenant;

import java.util.UUID;

/** Thread-local holder for the current tenant ID. Always call clear() in a finally block. */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    /** Returns the tenant ID bound to the current thread, or null if not set. */
    public static UUID getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Binds the given tenant ID to the current thread.
     *
     * @param tenantId the tenant UUID to associate with this thread
     */
    public static void setCurrentTenantId(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        CURRENT_TENANT.set(tenantId);
    }

    /** Removes the tenant context from the current thread. */
    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /** Returns true if a tenant ID is currently set on this thread. */
    public static boolean isPresent() {
        return CURRENT_TENANT.get() != null;
    }
}
