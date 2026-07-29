package com.outreach.platform.auth.model;

/**
 * Represents the role a user holds within a tenant.
 * <p>
 * PLATFORM_ADMIN is a cross-tenant role that grants unrestricted access
 * across all tenants. Only existing Platform_Admins can grant this role.
 */
public enum TenantRole {
    ADMIN,
    PMO,
    POC,
    PLATFORM_ADMIN
}
