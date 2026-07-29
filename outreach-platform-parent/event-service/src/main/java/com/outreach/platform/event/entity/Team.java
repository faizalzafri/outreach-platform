package com.outreach.platform.event.entity;

import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing a team within a tenant.
 * Teams group users for collaboration and resource sharing.
 * Maps to the {@code teams} table.
 */
@Entity
@Table(name = "teams")
@Getter
@Setter
public class Team extends TenantAwareBaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;
}
