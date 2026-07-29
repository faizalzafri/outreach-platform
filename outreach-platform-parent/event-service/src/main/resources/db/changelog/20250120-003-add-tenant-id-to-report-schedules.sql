--liquibase formatted sql

--changeset event-service:20250120-003-add-tenant-id-to-report-schedules
--comment: Add tenant_id column to report_schedules table for multi-tenant isolation
ALTER TABLE report_schedules
    ADD COLUMN tenant_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
--rollback ALTER TABLE report_schedules DROP COLUMN tenant_id;

--changeset event-service:20250120-003-add-fk-report-schedules-tenant
--comment: Add foreign key constraint from report_schedules.tenant_id to tenants table
ALTER TABLE report_schedules
    ADD CONSTRAINT fk_report_schedules_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id);
--rollback ALTER TABLE report_schedules DROP CONSTRAINT fk_report_schedules_tenant;

--changeset event-service:20250120-003-add-idx-report-schedules-tenant
--comment: Add composite index on (tenant_id, id) for tenant-scoped queries
CREATE INDEX idx_report_schedules_tenant ON report_schedules(tenant_id, id);
--rollback DROP INDEX IF EXISTS idx_report_schedules_tenant;

--changeset event-service:20250120-003-drop-default-report-schedules-tenant
--comment: Remove DEFAULT after migrating existing rows to Default Tenant
ALTER TABLE report_schedules ALTER COLUMN tenant_id DROP DEFAULT;
--rollback ALTER TABLE report_schedules ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-0000-0000-000000000001';
