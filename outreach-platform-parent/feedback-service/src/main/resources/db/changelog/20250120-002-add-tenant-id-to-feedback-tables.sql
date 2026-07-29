--liquibase formatted sql

--changeset feedback-service:20250120-002-ensure-tenants-table-exists
--comment: Ensure tenants table exists in shared schema (created by auth-service or event-service migration)
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'tenants'
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
--rollback SELECT 1;

--changeset feedback-service:20250120-002-seed-default-tenant
--comment: Seed Default Organization tenant if not already present
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM tenants WHERE id = '00000000-0000-0000-0000-000000000001'
INSERT INTO tenants (id, name, slug, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Organization', 'default', 'ACTIVE', 'system');
--rollback DELETE FROM tenants WHERE id = '00000000-0000-0000-0000-000000000001';

--changeset feedback-service:20250120-002-add-tenant-id-to-volunteer-feedback
--comment: Add tenant_id column to volunteer_feedback table for multi-tenant isolation (skipped if already added by event-service migration)
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'volunteer_feedback' AND column_name = 'tenant_id'
ALTER TABLE volunteer_feedback
    ADD COLUMN tenant_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
--rollback ALTER TABLE volunteer_feedback DROP COLUMN IF EXISTS tenant_id;

--changeset feedback-service:20250120-002-add-fk-volunteer-feedback-tenant
--comment: Add foreign key constraint from volunteer_feedback.tenant_id to tenants table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name = 'volunteer_feedback' AND constraint_name = 'fk_volunteer_feedback_tenant'
ALTER TABLE volunteer_feedback
    ADD CONSTRAINT fk_volunteer_feedback_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id);
--rollback ALTER TABLE volunteer_feedback DROP CONSTRAINT IF EXISTS fk_volunteer_feedback_tenant;

--changeset feedback-service:20250120-002-add-idx-volunteer-feedback-tenant
--comment: Add composite index on (tenant_id, id) for tenant-scoped queries
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'volunteer_feedback' AND indexname = 'idx_volunteer_feedback_tenant'
CREATE INDEX idx_volunteer_feedback_tenant ON volunteer_feedback(tenant_id, id);
--rollback DROP INDEX IF EXISTS idx_volunteer_feedback_tenant;

--changeset feedback-service:20250120-002-drop-default-volunteer-feedback-tenant
--comment: Remove DEFAULT after migrating existing rows to Default Tenant
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'volunteer_feedback' AND column_name = 'tenant_id' AND column_default IS NOT NULL
ALTER TABLE volunteer_feedback ALTER COLUMN tenant_id DROP DEFAULT;
--rollback ALTER TABLE volunteer_feedback ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-0000-0000-000000000001';
