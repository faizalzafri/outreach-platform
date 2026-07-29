package com.outreach.platform.common.tenant;

import jakarta.persistence.PrePersist;

/**
 * JPA entity lifecycle listener that automatically assigns the current tenant ID
 * to any {@link TenantAwareBaseEntity} being persisted.
 * <p>
 * This listener reads the tenant identity from {@link TenantContext} and sets it on the
 * entity's {@code tenantId} field before the INSERT statement executes. If no tenant
 * context is present, the listener throws an {@link IllegalStateException} to prevent
 * persisting tenant-scoped data without proper tenant association.
 * <p>
 * <strong>Execution order:</strong> This listener is declared on {@link TenantAwareBaseEntity}
 * before the {@code AuditingEntityListener} inherited from {@code BaseEntity}. JPA processes
 * {@code @EntityListeners} in declaration order from subclass to superclass, so tenant
 * assignment fires before audit fields (created_at, updated_at) are populated.
 *
 * @see TenantContext
 * @see TenantAwareBaseEntity
 */
public class TenantEntityListener {

    /**
     * Sets the {@code tenantId} field on the entity from the current {@link TenantContext}.
     * <p>
     * Called automatically by the JPA provider before an INSERT. If the entity is an
     * instance of {@link TenantAwareBaseEntity} and the tenant context is populated,
     * the tenant ID is assigned. If the tenant context is absent, persistence is aborted
     * with an exception.
     *
     * @param entity the entity being persisted
     * @throws IllegalStateException if {@link TenantContext#isPresent()} returns {@code false}
     */
    @PrePersist
    public void setTenantId(Object entity) {
        if (!TenantContext.isPresent()) {
            throw new IllegalStateException(
                    "Cannot persist entity without tenant context. " +
                    "Ensure TenantContext is populated before persisting tenant-scoped entities."
            );
        }

        if (entity instanceof TenantAwareBaseEntity tenantEntity) {
            tenantEntity.setTenantId(TenantContext.getCurrentTenantId());
        }
    }
}
