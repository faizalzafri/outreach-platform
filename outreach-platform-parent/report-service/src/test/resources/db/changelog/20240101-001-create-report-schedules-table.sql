--liquibase formatted sql

--changeset report-service:20240101-001-create-tenants-table-for-tests
--comment: report_schedules.tenant_id has a foreign key to tenants(id) in production (owned by event-service/auth-service migrations against the shared schema); this service's own isolated tests need a minimal version of that table to satisfy the constraint.
CREATE TABLE "tenants" (
    "id"     UUID         NOT NULL DEFAULT gen_random_uuid(),
    "name"   VARCHAR(100) NOT NULL,
    "slug"   VARCHAR(50)  NOT NULL,
    "status" VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT "pk_tenants" PRIMARY KEY ("id")
);
--rollback DROP TABLE IF EXISTS "tenants";

--changeset report-service:20240101-001-create-report-schedules-table-for-tests
--comment: report_schedules is created by event-service's 20240101-012-create-report-schedules-table.sql against the shared schema; report-service has Liquibase disabled in its main config entirely (read-only-by-design, schema owned elsewhere), so this test-only changelog (mirroring feedback-service's existing db.changelog-test.xml pattern) is the only way this service's own isolated tests can validate ReportScheduleEntity's mapping against a real engine. Reflects the table's FINAL production shape (original create + event-service's later tenant_id-add and audit-column-add migrations), not the migration history.
CREATE TABLE "report_schedules" (
    "id"                UUID            NOT NULL DEFAULT gen_random_uuid(),
    "tenant_id"         UUID            NOT NULL,
    "name"              VARCHAR(100)    NOT NULL,
    "report_type"       VARCHAR(50)     NOT NULL,
    "cron_expression"   VARCHAR(100)    NOT NULL,
    "export_format"     VARCHAR(20)     NOT NULL DEFAULT 'PDF',
    "filter_criteria"   JSONB,
    "recipients"        JSONB,
    "status"            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    "last_run_at"       TIMESTAMP WITH TIME ZONE,
    "next_run_at"       TIMESTAMP WITH TIME ZONE,
    "created_at"        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "updated_at"        TIMESTAMP WITH TIME ZONE,
    "created_by"        VARCHAR(100),
    "updated_by"        VARCHAR(100),
    "version"           BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT "pk_report_schedules" PRIMARY KEY ("id"),
    CONSTRAINT "fk_report_schedules_tenant" FOREIGN KEY ("tenant_id") REFERENCES "tenants" ("id"),
    CONSTRAINT "chk_report_schedules_type" CHECK ("report_type" IN ('BY_EVENT', 'BY_BENEFICIARY', 'BY_CITY', 'BY_POC', 'CUSTOM')),
    CONSTRAINT "chk_report_schedules_format" CHECK ("export_format" IN ('PDF', 'CSV', 'EXCEL')),
    CONSTRAINT "chk_report_schedules_status" CHECK ("status" IN ('ACTIVE', 'PAUSED', 'COMPLETED'))
);
--rollback DROP TABLE IF EXISTS "report_schedules";
