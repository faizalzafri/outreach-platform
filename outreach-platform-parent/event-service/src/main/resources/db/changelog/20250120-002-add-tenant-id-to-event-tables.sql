--liquibase formatted sql

--changeset event-service:20250120-002-ensure-tenants-table-exists
--comment: Ensure tenants table exists in shared schema (created by auth-service migration)
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

--changeset event-service:20250120-002-seed-default-tenant
--comment: Seed Default Organization tenant if not already present
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM tenants WHERE id = '00000000-0000-0000-0000-000000000001'
INSERT INTO tenants (id, name, slug, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Organization', 'default', 'ACTIVE', 'system');
--rollback DELETE FROM tenants WHERE id = '00000000-0000-0000-0000-000000000001';

--changeset event-service:20250120-002-add-tenant-id-to-users
--comment: Add tenant_id column to users table for multi-tenant isolation
ALTER TABLE "users" ADD COLUMN "tenant_id" UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
ALTER TABLE "users" ADD CONSTRAINT "fk_users_tenant" FOREIGN KEY ("tenant_id") REFERENCES tenants(id);
CREATE INDEX "idx_users_tenant_id" ON "users" ("tenant_id", "id");
--rollback DROP INDEX IF EXISTS "idx_users_tenant_id"; ALTER TABLE "users" DROP CONSTRAINT IF EXISTS "fk_users_tenant"; ALTER TABLE "users" DROP COLUMN IF EXISTS "tenant_id";

--changeset event-service:20250120-002-add-tenant-id-to-events
--comment: Add tenant_id column to events table for multi-tenant isolation
ALTER TABLE "events" ADD COLUMN "tenant_id" UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
ALTER TABLE "events" ADD CONSTRAINT "fk_events_tenant" FOREIGN KEY ("tenant_id") REFERENCES tenants(id);
CREATE INDEX "idx_events_tenant_id" ON "events" ("tenant_id", "id");
--rollback DROP INDEX IF EXISTS "idx_events_tenant_id"; ALTER TABLE "events" DROP CONSTRAINT IF EXISTS "fk_events_tenant"; ALTER TABLE "events" DROP COLUMN IF EXISTS "tenant_id";

--changeset event-service:20250120-002-add-tenant-id-to-beneficiaries
--comment: Add tenant_id column to beneficiaries table for multi-tenant isolation
ALTER TABLE "beneficiaries" ADD COLUMN "tenant_id" UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
ALTER TABLE "beneficiaries" ADD CONSTRAINT "fk_beneficiaries_tenant" FOREIGN KEY ("tenant_id") REFERENCES tenants(id);
CREATE INDEX "idx_beneficiaries_tenant_id" ON "beneficiaries" ("tenant_id", "id");
--rollback DROP INDEX IF EXISTS "idx_beneficiaries_tenant_id"; ALTER TABLE "beneficiaries" DROP CONSTRAINT IF EXISTS "fk_beneficiaries_tenant"; ALTER TABLE "beneficiaries" DROP COLUMN IF EXISTS "tenant_id";

--changeset event-service:20250120-002-add-tenant-id-to-event-beneficiary
--comment: Add tenant_id column to event_beneficiary junction table for multi-tenant isolation
ALTER TABLE "event_beneficiary" ADD COLUMN "tenant_id" UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
ALTER TABLE "event_beneficiary" ADD CONSTRAINT "fk_event_beneficiary_tenant" FOREIGN KEY ("tenant_id") REFERENCES tenants(id);
CREATE INDEX "idx_event_beneficiary_tenant_id" ON "event_beneficiary" ("tenant_id", "event_id", "beneficiary_id");
--rollback DROP INDEX IF EXISTS "idx_event_beneficiary_tenant_id"; ALTER TABLE "event_beneficiary" DROP CONSTRAINT IF EXISTS "fk_event_beneficiary_tenant"; ALTER TABLE "event_beneficiary" DROP COLUMN IF EXISTS "tenant_id";

--changeset event-service:20250120-002-add-tenant-id-to-volunteers
--comment: Add tenant_id column to volunteers table for multi-tenant isolation
ALTER TABLE "volunteers" ADD COLUMN "tenant_id" UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
ALTER TABLE "volunteers" ADD CONSTRAINT "fk_volunteers_tenant" FOREIGN KEY ("tenant_id") REFERENCES tenants(id);
CREATE INDEX "idx_volunteers_tenant_id" ON "volunteers" ("tenant_id", "id");
--rollback DROP INDEX IF EXISTS "idx_volunteers_tenant_id"; ALTER TABLE "volunteers" DROP CONSTRAINT IF EXISTS "fk_volunteers_tenant"; ALTER TABLE "volunteers" DROP COLUMN IF EXISTS "tenant_id";

--changeset event-service:20250120-002-add-tenant-id-to-event-enrollment
--comment: Add tenant_id column to event_enrollment table for multi-tenant isolation
ALTER TABLE "event_enrollment" ADD COLUMN "tenant_id" UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
ALTER TABLE "event_enrollment" ADD CONSTRAINT "fk_event_enrollment_tenant" FOREIGN KEY ("tenant_id") REFERENCES tenants(id);
CREATE INDEX "idx_event_enrollment_tenant_id" ON "event_enrollment" ("tenant_id", "id");
--rollback DROP INDEX IF EXISTS "idx_event_enrollment_tenant_id"; ALTER TABLE "event_enrollment" DROP CONSTRAINT IF EXISTS "fk_event_enrollment_tenant"; ALTER TABLE "event_enrollment" DROP COLUMN IF EXISTS "tenant_id";

--changeset event-service:20250120-002-add-tenant-id-to-poc-assignments
--comment: Add tenant_id column to poc_assignments table for multi-tenant isolation
ALTER TABLE "poc_assignments" ADD COLUMN "tenant_id" UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
ALTER TABLE "poc_assignments" ADD CONSTRAINT "fk_poc_assignments_tenant" FOREIGN KEY ("tenant_id") REFERENCES tenants(id);
CREATE INDEX "idx_poc_assignments_tenant_id" ON "poc_assignments" ("tenant_id", "id");
--rollback DROP INDEX IF EXISTS "idx_poc_assignments_tenant_id"; ALTER TABLE "poc_assignments" DROP CONSTRAINT IF EXISTS "fk_poc_assignments_tenant"; ALTER TABLE "poc_assignments" DROP COLUMN IF EXISTS "tenant_id";

--changeset event-service:20250120-002-add-tenant-id-to-volunteer-feedback
--comment: Add tenant_id column to volunteer_feedback table for multi-tenant isolation
ALTER TABLE "volunteer_feedback" ADD COLUMN "tenant_id" UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
ALTER TABLE "volunteer_feedback" ADD CONSTRAINT "fk_volunteer_feedback_tenant" FOREIGN KEY ("tenant_id") REFERENCES tenants(id);
CREATE INDEX "idx_volunteer_feedback_tenant_id" ON "volunteer_feedback" ("tenant_id", "id");
--rollback DROP INDEX IF EXISTS "idx_volunteer_feedback_tenant_id"; ALTER TABLE "volunteer_feedback" DROP CONSTRAINT IF EXISTS "fk_volunteer_feedback_tenant"; ALTER TABLE "volunteer_feedback" DROP COLUMN IF EXISTS "tenant_id";

--changeset event-service:20250120-002-drop-tenant-id-defaults
--comment: Remove DEFAULT from tenant_id columns after migration (existing rows already assigned to Default_Tenant)
ALTER TABLE "users" ALTER COLUMN "tenant_id" DROP DEFAULT;
ALTER TABLE "events" ALTER COLUMN "tenant_id" DROP DEFAULT;
ALTER TABLE "beneficiaries" ALTER COLUMN "tenant_id" DROP DEFAULT;
ALTER TABLE "event_beneficiary" ALTER COLUMN "tenant_id" DROP DEFAULT;
ALTER TABLE "volunteers" ALTER COLUMN "tenant_id" DROP DEFAULT;
ALTER TABLE "event_enrollment" ALTER COLUMN "tenant_id" DROP DEFAULT;
ALTER TABLE "poc_assignments" ALTER COLUMN "tenant_id" DROP DEFAULT;
ALTER TABLE "volunteer_feedback" ALTER COLUMN "tenant_id" DROP DEFAULT;
--rollback ALTER TABLE "users" ALTER COLUMN "tenant_id" SET DEFAULT '00000000-0000-0000-0000-000000000001'; ALTER TABLE "events" ALTER COLUMN "tenant_id" SET DEFAULT '00000000-0000-0000-0000-000000000001'; ALTER TABLE "beneficiaries" ALTER COLUMN "tenant_id" SET DEFAULT '00000000-0000-0000-0000-000000000001'; ALTER TABLE "event_beneficiary" ALTER COLUMN "tenant_id" SET DEFAULT '00000000-0000-0000-0000-000000000001'; ALTER TABLE "volunteers" ALTER COLUMN "tenant_id" SET DEFAULT '00000000-0000-0000-0000-000000000001'; ALTER TABLE "event_enrollment" ALTER COLUMN "tenant_id" SET DEFAULT '00000000-0000-0000-0000-000000000001'; ALTER TABLE "poc_assignments" ALTER COLUMN "tenant_id" SET DEFAULT '00000000-0000-0000-0000-000000000001'; ALTER TABLE "volunteer_feedback" ALTER COLUMN "tenant_id" SET DEFAULT '00000000-0000-0000-0000-000000000001';
