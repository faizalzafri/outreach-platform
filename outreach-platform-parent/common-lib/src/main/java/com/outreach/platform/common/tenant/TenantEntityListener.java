package com.outreach.platform.common.tenant;

import jakarta.persistence.PrePersist;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * JPA entity lifecycle listener that automatically assigns the current tenant ID
 * to any tenant-scoped entity being persisted.
 * <p>
 * This listener reads the tenant identity from {@link TenantContext} and sets it on the
 * entity's {@code tenantId} field before the INSERT statement executes. If no tenant
 * context is present, the listener throws an {@link IllegalStateException} to prevent
 * persisting tenant-scoped data without proper tenant association.
 * <p>
 * The listener supports two entity styles:
 * <ul>
 *   <li>Entities extending {@link TenantAwareBaseEntity} — uses the package-private setter</li>
 *   <li>Standalone entities with a {@code tenantId} field — uses reflection to assign the value</li>
 * </ul>
 * <p>
 * <strong>Execution order:</strong> This listener is declared before the
 * {@code AuditingEntityListener} so that tenant assignment fires before audit fields
 * (created_at, updated_at) are populated.
 *
 * @see TenantContext
 * @see TenantAwareBaseEntity
 */
public class TenantEntityListener {

    /**
     * Sets the {@code tenantId} field on the entity from the current {@link TenantContext}.
     * <p>
     * Called automatically by the JPA provider before an INSERT. If the entity extends
     * {@link TenantAwareBaseEntity}, the package-private setter is used. Otherwise, the
     * listener reflectively looks for a {@code tenantId} field and sets it. If the tenant
     * context is absent, persistence is aborted with an exception.
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

        UUID currentTenantId = TenantContext.getCurrentTenantId();

        if (entity instanceof TenantAwareBaseEntity tenantEntity) {
            tenantEntity.setTenantId(currentTenantId);
        } else {
            setTenantIdViaReflection(entity, currentTenantId);
        }
    }

    private void setTenantIdViaReflection(Object entity, UUID tenantId) {
        try {
            Field field = findTenantIdField(entity.getClass());
            if (field != null) {
                field.setAccessible(true);
                if (field.get(entity) == null) {
                    field.set(entity, tenantId);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Failed to set tenantId on entity " + entity.getClass().getSimpleName(), e
            );
        }
    }

    private Field findTenantIdField(Class<?> clazz) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField("tenantId");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
