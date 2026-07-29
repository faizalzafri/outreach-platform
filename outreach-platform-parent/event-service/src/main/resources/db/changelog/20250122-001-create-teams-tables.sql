--liquibase formatted sql

--changeset event-service:20250122-001-create-teams-table
--comment: Create teams table for organizing users within a tenant
CREATE TABLE teams (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(500),
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(255) NOT NULL,
    last_modified_by    VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_teams_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_teams_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_teams_tenant ON teams(tenant_id);
--rollback DROP TABLE IF EXISTS teams;

--changeset event-service:20250122-001-create-team-memberships-table
--comment: Create team_memberships table for assigning users to teams
CREATE TABLE team_memberships (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id             UUID NOT NULL,
    user_id             UUID NOT NULL,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(255) NOT NULL,
    last_modified_by    VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_team_memberships_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT uq_team_memberships_team_user UNIQUE (team_id, user_id)
);

CREATE INDEX idx_team_memberships_team ON team_memberships(team_id);
CREATE INDEX idx_team_memberships_user ON team_memberships(user_id);
--rollback DROP TABLE IF EXISTS team_memberships;
