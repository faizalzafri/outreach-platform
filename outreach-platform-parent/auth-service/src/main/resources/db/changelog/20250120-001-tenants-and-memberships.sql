--liquibase formatted sql

--changeset auth-service:20250120-001-create-tenants-table
--comment: Create tenants table for multi-tenant SaaS support
CREATE TABLE IF NOT EXISTS tenants (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL UNIQUE,
    slug                VARCHAR(50)  NOT NULL UNIQUE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(255) NOT NULL,
    last_modified_by    VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED')),
    CONSTRAINT chk_tenant_name_length CHECK (LENGTH(name) BETWEEN 3 AND 100),
    CONSTRAINT chk_tenant_slug_format CHECK (slug ~ '^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$')
);
--rollback DROP TABLE IF EXISTS tenants;

--changeset auth-service:20250120-001-create-tenant-memberships-table
--comment: Create tenant_memberships table linking users to tenants with roles
CREATE TABLE IF NOT EXISTS tenant_memberships (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    user_id             UUID NOT NULL,
    role                VARCHAR(20) NOT NULL,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(255) NOT NULL,
    last_modified_by    VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_tenant_user_role UNIQUE (tenant_id, user_id, role),
    CONSTRAINT chk_membership_role CHECK (role IN ('ADMIN', 'PMO', 'POC'))
);
--rollback DROP TABLE IF EXISTS tenant_memberships;

--changeset auth-service:20250120-001-create-tenant-membership-indexes
--comment: Add indexes for tenant_memberships lookups
CREATE INDEX idx_tenant_memberships_tenant ON tenant_memberships(tenant_id);
CREATE INDEX idx_tenant_memberships_user ON tenant_memberships(user_id);
--rollback DROP INDEX IF EXISTS idx_tenant_memberships_tenant; DROP INDEX IF EXISTS idx_tenant_memberships_user;

--changeset auth-service:20250120-001-seed-default-tenant
--comment: Seed the Default Organization tenant for backward compatibility
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM tenants WHERE id = '00000000-0000-0000-0000-000000000001'
INSERT INTO tenants (id, name, slug, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Organization', 'default', 'ACTIVE', 'system');
--rollback DELETE FROM tenants WHERE id = '00000000-0000-0000-0000-000000000001';
