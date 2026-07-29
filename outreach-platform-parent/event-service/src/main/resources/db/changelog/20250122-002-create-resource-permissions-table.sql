--liquibase formatted sql

--changeset event-service:20250122-002-create-resource-permissions-table
--comment: Create resource_permissions table for fine-grained resource sharing within tenants
CREATE TABLE resource_permissions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    resource_type       VARCHAR(50) NOT NULL,
    resource_id         UUID NOT NULL,
    visibility          VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    owner_user_id       UUID NOT NULL,
    granted_team_id     UUID,
    permission_level    VARCHAR(20) NOT NULL DEFAULT 'VIEW',
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(255) NOT NULL,
    last_modified_by    VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_resource_permissions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_resource_permissions_team FOREIGN KEY (granted_team_id) REFERENCES teams(id),
    CONSTRAINT chk_resource_permissions_visibility CHECK (visibility IN ('PRIVATE', 'TEAM', 'TENANT')),
    CONSTRAINT chk_resource_permissions_level CHECK (permission_level IN ('VIEW', 'EDIT', 'MANAGE'))
);

CREATE INDEX idx_resource_permissions_tenant_resource ON resource_permissions(tenant_id, resource_type, resource_id);
CREATE INDEX idx_resource_permissions_team ON resource_permissions(granted_team_id);
CREATE INDEX idx_resource_permissions_owner ON resource_permissions(owner_user_id);
--rollback DROP TABLE IF EXISTS resource_permissions;
