package com.outreach.platform.event.entity;

import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * JPA entity representing the membership of a user within a team.
 * Maps to the {@code team_memberships} table.
 */
@Entity
@Table(name = "team_memberships")
@Getter
@Setter
public class TeamMembership extends TenantAwareBaseEntity {

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
