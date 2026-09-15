package com.outreach.platform.common.tenant;

import jakarta.persistence.PrePersist;

import java.lang.reflect.Field;
import java.util.UUID;

/** JPA lifecycle listener that assigns the current tenant ID to entities before persist. */
public class TenantEntityListener {

    /**
     * Sets the tenantId field on the entity from TenantContext.
     *
     * @param entity the entity being persisted
     * @throws IllegalStateException if no tenant context is present
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
