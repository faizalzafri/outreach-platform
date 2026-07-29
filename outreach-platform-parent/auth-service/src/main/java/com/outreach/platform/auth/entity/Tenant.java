package com.outreach.platform.auth.entity;

import com.outreach.platform.auth.model.TenantStatus;
import com.outreach.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing a tenant in the multi-tenant platform.
 * Maps to the {@code tenants} table created by the Liquibase migration.
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
public class Tenant extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 50)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;
}
