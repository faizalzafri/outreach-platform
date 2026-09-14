--liquibase formatted sql

--changeset notification-service:20250120-001-ensure-notification-templates-table-exists
--comment: notification_templates is created by event-service's 20240101-010-create-notification-templates-table.sql against the shared schema; notification-service's own isolated Testcontainers tests never saw it and Hibernate's ddl-auto=validate failed with "missing table [notification_templates]" - mirrors that migration's original (pre-tenant-id, pre-platform-hardening) column set exactly, idempotent for real deployments where event-service already created it. The later changesets in this changelog (tenant_id add, audit-column add) then apply on top exactly as they do in production.
CREATE TABLE IF NOT EXISTS "notification_templates" (
    "id"                UUID            NOT NULL DEFAULT gen_random_uuid(),
    "name"              VARCHAR(100)    NOT NULL,
    "type"              VARCHAR(50)     NOT NULL DEFAULT 'EMAIL',
    "subject_template"  VARCHAR(100),
    "body_template"     TEXT            NOT NULL,
    "engine"            VARCHAR(50)     NOT NULL DEFAULT 'THYMELEAF',
    "variables_schema"  JSONB,
    "active"            BOOLEAN         NOT NULL DEFAULT TRUE,
    "version"           INTEGER         NOT NULL DEFAULT 1,
    "created_at"        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "updated_at"        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "created_by"        VARCHAR(100),

    CONSTRAINT "pk_notification_templates" PRIMARY KEY ("id"),
    CONSTRAINT "uk_notification_templates_name" UNIQUE ("name"),
    CONSTRAINT "chk_notification_templates_type" CHECK ("type" IN ('EMAIL', 'SMS', 'PUSH')),
    CONSTRAINT "chk_notification_templates_engine" CHECK ("engine" IN ('THYMELEAF', 'FREEMARKER'))
);
--rollback DROP TABLE IF EXISTS "notification_templates";

--changeset notification-service:20250120-001-ensure-notification-schedules-table-exists
--comment: notification_schedules is created by event-service's 20240101-011-create-notification-schedules-table.sql. Omits that migration's foreign keys to notification_templates(id) and events(id) deliberately - the first is redundant with application-level integrity for an isolated test schema, the second can't exist here at all since events is owned entirely by event-service and this changeset must stay self-contained for notification-service's own isolated tests.
CREATE TABLE IF NOT EXISTS "notification_schedules" (
    "id"                UUID            NOT NULL DEFAULT gen_random_uuid(),
    "template_id"       UUID            NOT NULL,
    "event_id"          UUID,
    "trigger_type"      VARCHAR(50)     NOT NULL DEFAULT 'IMMEDIATE',
    "cron_expression"   VARCHAR(100),
    "scheduled_at"      TIMESTAMP WITH TIME ZONE,
    "status"            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    "recipient_filter"  JSONB,
    "created_at"        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "created_by"        VARCHAR(100),

    CONSTRAINT "pk_notification_schedules" PRIMARY KEY ("id"),
    CONSTRAINT "chk_notification_schedules_trigger" CHECK ("trigger_type" IN ('IMMEDIATE', 'SCHEDULED', 'EVENT_LIFECYCLE')),
    CONSTRAINT "chk_notification_schedules_status" CHECK ("status" IN ('PENDING', 'EXECUTING', 'COMPLETED', 'CANCELLED'))
);
--rollback DROP TABLE IF EXISTS "notification_schedules";
