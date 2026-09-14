--liquibase formatted sql

--changeset event-service:20250122-006-add-audit-columns-to-report-schedules
--comment: Adds columns needed to migrate ReportScheduleEntity onto TenantAwareBaseEntity (docs/specs/platform-hardening/) - report_schedules has created_at/created_by but no updated_at/updated_by/version. Owned here because report-service has Liquibase disabled (read-only datasource, schema managed by event-service).
ALTER TABLE "report_schedules" ADD COLUMN "updated_at" TIMESTAMP WITH TIME ZONE;
ALTER TABLE "report_schedules" ADD COLUMN "updated_by" VARCHAR(100);
ALTER TABLE "report_schedules" ADD COLUMN "version" BIGINT NOT NULL DEFAULT 0;
--rollback ALTER TABLE "report_schedules" DROP COLUMN IF EXISTS "updated_at"; ALTER TABLE "report_schedules" DROP COLUMN IF EXISTS "updated_by"; ALTER TABLE "report_schedules" DROP COLUMN IF EXISTS "version";
