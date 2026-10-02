package com.outreach.platform.event.entity;

import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import com.outreach.platform.event.model.AccessLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/** A team given access to an event: its POCs can work on the event as if assigned to it. */
@Entity
@Table(name = "event_team_access")
public class EventTeamAccess extends TenantAwareBaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 10)
    private AccessLevel accessLevel;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }
}
