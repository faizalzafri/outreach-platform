package com.outreach.platform.common.tenant;

import com.outreach.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

/**
 * Abstract mapped superclass for all tenant-scoped JPA entities.
 * <p>
 * Extends {@link BaseEntity} (which provides UUID primary key, audit fields, and optimistic
 * locking) and adds multi-tenant infrastructure:
 * <ul>
 *   <li>A non-nullable, immutable {@code tenant_id} column linking each row to a specific tenant</li>
 *   <li>A Hibernate {@code @FilterDef}/{@code @Filter} pair that automatically appends
 *       {@code WHERE tenant_id = :tenantId} to all queries when the filter is enabled</li>
 *   <li>A {@link TenantEntityListener} that auto-populates {@code tenantId} from
 *       {@link TenantContext} on persist, preventing records from being created without
 *       proper tenant association</li>
 * </ul>
 * <p>
 * All business entities that hold tenant-specific data should extend this class instead of
 * {@link BaseEntity} directly.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @Entity
 * @Table(name = "events")
 * public class EventEntity extends TenantAwareBaseEntity {
 *     // entity-specific fields
 * }
 * }</pre>
 *
 * @see TenantContext
 * @see TenantEntityListener
 * @see TenantConstants#TENANT_FILTER_NAME
 */
@MappedSuperclass
@FilterDef(
        name = TenantConstants.TENANT_FILTER_NAME,
        parameters = @ParamDef(name = "tenantId", type = UUID.class)
)
@Filter(
        name = TenantConstants.TENANT_FILTER_NAME,
        condition = "tenant_id = :tenantId"
)
@EntityListeners(TenantEntityListener.class)
@Getter
public abstract class TenantAwareBaseEntity extends BaseEntity {

    /**
     * The tenant identifier for this entity.
     * <p>
     * This column is non-nullable and immutable — once assigned during persist via
     * {@link TenantEntityListener}, it cannot be changed. The value is set automatically
     * from {@link TenantContext#getCurrentTenantId()} and should never be set manually.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /**
     * Package-private setter used exclusively by {@link TenantEntityListener} to assign
     * the tenant ID during the {@code @PrePersist} lifecycle callback.
     * <p>
     * Service code should never call this method directly — tenant assignment is automatic.
     *
     * @param tenantId the tenant UUID to assign; must not be {@code null}
     */
    void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
}
