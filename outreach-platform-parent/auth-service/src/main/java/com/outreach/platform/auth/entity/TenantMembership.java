package com.outreach.platform.auth.entity;

import com.outreach.platform.auth.model.TenantRole;
import com.outreach.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** JPA entity representing a user's membership and role within a tenant. */
@Entity
@Table(name = "tenant_memberships")
@Getter
@Setter
public class TenantMembership extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private TenantRole role;
}
