package com.outreach.platform.common.tenant;

import java.util.UUID;

/** Shared constants for multi-tenant infrastructure across all platform services. */
public final class TenantConstants {

    private TenantConstants() {
    }

    /** HTTP header used to propagate tenant identity between gateway and downstream services. */
    public static final String X_TENANT_ID_HEADER = "X-Tenant-ID";

    /** Name of the Hibernate filter applied to all tenant-scoped entities. */
    public static final String TENANT_FILTER_NAME = "tenantFilter";

    /** Default tenant UUID assigned to pre-existing data during migration. */
    public static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
}
