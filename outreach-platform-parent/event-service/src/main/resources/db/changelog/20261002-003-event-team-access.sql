--liquibase formatted sql

--changeset event-service:20261002-003-event-team-access
--comment: Sharing means one thing here: a team gets access to an event, so its POCs can work on it
--         without being assigned one by one. This replaces the generic resource_permissions table,
--         whose resource types (sequences, templates, contact lists) this platform never had.
CREATE TABLE event_team_access (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    event_id            UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    team_id             UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    access_level        VARCHAR(10) NOT NULL CHECK (access_level IN ('VIEW', 'EDIT')),
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(255) NOT NULL,
    last_modified_by    VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_event_team_access UNIQUE (event_id, team_id)
);
CREATE INDEX idx_event_team_access_team ON event_team_access(team_id);
DROP TABLE resource_permissions;
