--liquibase formatted sql

--changeset outreach-platform:20240101-012-create-report-schedules-table
--comment: Create report_schedules table - scheduled report generation and export jobs
CREATE TABLE "report_schedules" (
    "id"                UUID            NOT NULL DEFAULT gen_random_uuid(),
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
    "created_by"        VARCHAR(100),

    CONSTRAINT "pk_report_schedules" PRIMARY KEY ("id"),
    CONSTRAINT "chk_report_schedules_type" CHECK ("report_type" IN ('BY_EVENT', 'BY_BENEFICIARY', 'BY_CITY', 'BY_POC', 'CUSTOM')),
    CONSTRAINT "chk_report_schedules_format" CHECK ("export_format" IN ('PDF', 'CSV', 'EXCEL')),
    CONSTRAINT "chk_report_schedules_status" CHECK ("status" IN ('ACTIVE', 'PAUSED', 'COMPLETED'))
);

COMMENT ON TABLE "report_schedules" IS 'Scheduled report generation with configurable format and recipients';

--rollback DROP TABLE IF EXISTS "report_schedules";
