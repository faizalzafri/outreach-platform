--liquibase formatted sql

--changeset event-service:20250122-005-ensure-tenant-memberships-table-exists
--comment: TenantMembershipEntity maps to tenant_memberships, which only auth-service's migrations create; event-service's own isolated Testcontainers tests never saw it and failed Hibernate schema validation with "missing table [tenant_memberships]" - mirrors auth-service's 20250120-001-tenants-and-memberships.sql exactly, idempotent for real deployments where auth-service already created it
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

--changeset event-service:20250122-005-ensure-tenant-membership-indexes-exist
--comment: Mirrors auth-service's indexes, idempotent via preconditions since IF NOT EXISTS is not valid for CREATE INDEX in this Postgres version's syntax path used elsewhere in this changelog
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'tenant_memberships' AND indexname = 'idx_tenant_memberships_tenant'
CREATE INDEX idx_tenant_memberships_tenant ON tenant_memberships(tenant_id);
CREATE INDEX idx_tenant_memberships_user ON tenant_memberships(user_id);
--rollback DROP INDEX IF EXISTS idx_tenant_memberships_tenant; DROP INDEX IF EXISTS idx_tenant_memberships_user;
