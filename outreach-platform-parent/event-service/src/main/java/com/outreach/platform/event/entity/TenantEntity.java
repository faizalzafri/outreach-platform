package com.outreach.platform.event.entity;

import com.outreach.platform.common.entity.BaseEntity;
import com.outreach.platform.event.model.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing a tenant in the multi-tenant platform.
 * This is a duplicate of the auth-service Tenant entity, kept in event-service
 * for service independence. Maps to the {@code tenants} table.
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
public class TenantEntity extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 50)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;
}
