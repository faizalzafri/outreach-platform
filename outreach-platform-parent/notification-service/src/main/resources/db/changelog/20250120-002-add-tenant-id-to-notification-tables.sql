--liquibase formatted sql

--changeset notification-service:20250120-002-ensure-tenants-table-exists
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

--changeset notification-service:20250120-002-seed-default-tenant
--comment: Seed Default Organization tenant if not already present
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM tenants WHERE id = '00000000-0000-0000-0000-000000000001'
INSERT INTO tenants (id, name, slug, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Organization', 'default', 'ACTIVE', 'system');
--rollback DELETE FROM tenants WHERE id = '00000000-0000-0000-0000-000000000001';

--changeset notification-service:20250120-002-add-tenant-id-to-notification-templates
--comment: Add tenant_id column to notification_templates table for multi-tenant isolation
ALTER TABLE notification_templates
    ADD COLUMN tenant_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
--rollback ALTER TABLE notification_templates DROP COLUMN tenant_id;

--changeset notification-service:20250120-002-add-fk-notification-templates-tenant
--comment: Add foreign key constraint from notification_templates.tenant_id to tenants table
ALTER TABLE notification_templates
    ADD CONSTRAINT fk_notification_templates_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id);
--rollback ALTER TABLE notification_templates DROP CONSTRAINT fk_notification_templates_tenant;

--changeset notification-service:20250120-002-add-idx-notification-templates-tenant
--comment: Add composite index on (tenant_id, id) for tenant-scoped queries
CREATE INDEX idx_notification_templates_tenant ON notification_templates(tenant_id, id);
--rollback DROP INDEX IF EXISTS idx_notification_templates_tenant;

--changeset notification-service:20250120-002-drop-default-notification-templates-tenant
--comment: Remove DEFAULT after migrating existing rows to Default Tenant
ALTER TABLE notification_templates ALTER COLUMN tenant_id DROP DEFAULT;
--rollback ALTER TABLE notification_templates ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-0000-0000-000000000001';

--changeset notification-service:20250120-002-add-tenant-id-to-notification-schedules
--comment: Add tenant_id column to notification_schedules table for multi-tenant isolation
ALTER TABLE notification_schedules
    ADD COLUMN tenant_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
--rollback ALTER TABLE notification_schedules DROP COLUMN tenant_id;

--changeset notification-service:20250120-002-add-fk-notification-schedules-tenant
--comment: Add foreign key constraint from notification_schedules.tenant_id to tenants table
ALTER TABLE notification_schedules
    ADD CONSTRAINT fk_notification_schedules_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id);
--rollback ALTER TABLE notification_schedules DROP CONSTRAINT fk_notification_schedules_tenant;

--changeset notification-service:20250120-002-add-idx-notification-schedules-tenant
--comment: Add composite index on (tenant_id, id) for tenant-scoped queries
CREATE INDEX idx_notification_schedules_tenant ON notification_schedules(tenant_id, id);
--rollback DROP INDEX IF EXISTS idx_notification_schedules_tenant;

--changeset notification-service:20250120-002-drop-default-notification-schedules-tenant
--comment: Remove DEFAULT after migrating existing rows to Default Tenant
ALTER TABLE notification_schedules ALTER COLUMN tenant_id DROP DEFAULT;
--rollback ALTER TABLE notification_schedules ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-0000-0000-000000000001';
