package com.outreach.platform.common.tenant;

import java.util.UUID;

/**
 * Shared constants for multi-tenant infrastructure across all platform services.
 * <p>
 * These values are referenced by filters, aspects, entity listeners, cache key generators,
 * and messaging components to ensure consistent tenant identification throughout the system.
 */
public final class TenantConstants {

    private TenantConstants() {
        // utility class — not instantiable
    }

    /**
     * HTTP header name used to propagate tenant identity between the gateway and downstream services.
     * The gateway extracts the tenant ID from the JWT and forwards it in this header.
     */
    public static final String X_TENANT_ID_HEADER = "X-Tenant-ID";

    /**
     * Name of the Hibernate {@code @Filter} applied to all tenant-scoped entities.
     * Used in {@code @FilterDef} and {@code @Filter} annotations on {@code TenantAwareBaseEntity},
     * and enabled programmatically by {@code TenantFilterAspect}.
     */
    public static final String TENANT_FILTER_NAME = "tenantFilter";

    /**
     * Well-known UUID assigned to the default (legacy) tenant during migration.
     * All pre-existing data is associated with this tenant to maintain backward compatibility.
     */
    public static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
}
