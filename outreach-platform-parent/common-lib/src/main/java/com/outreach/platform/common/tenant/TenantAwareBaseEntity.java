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

/** Abstract mapped superclass for all tenant-scoped JPA entities, adding tenant_id column and Hibernate filter. */
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

    /** Non-nullable, immutable tenant identifier auto-assigned on persist via TenantEntityListener. */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /** Package-private setter used by TenantEntityListener during @PrePersist. */
    void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
}
