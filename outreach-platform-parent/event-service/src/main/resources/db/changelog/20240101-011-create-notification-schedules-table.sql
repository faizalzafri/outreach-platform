--liquibase formatted sql

--changeset outreach-platform:20240101-011-create-notification-schedules-table
--comment: Create notification_schedules table - scheduled and triggered notification dispatches
CREATE TABLE "notification_schedules" (
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
    CONSTRAINT "fk_notification_schedules_template" FOREIGN KEY ("template_id")
        REFERENCES "notification_templates" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_notification_schedules_event" FOREIGN KEY ("event_id")
        REFERENCES "events" ("id") ON DELETE SET NULL,
    CONSTRAINT "chk_notification_schedules_trigger" CHECK ("trigger_type" IN ('IMMEDIATE', 'SCHEDULED', 'EVENT_LIFECYCLE')),
    CONSTRAINT "chk_notification_schedules_status" CHECK ("status" IN ('PENDING', 'EXECUTING', 'COMPLETED', 'CANCELLED'))
);

COMMENT ON TABLE "notification_schedules" IS 'Notification dispatch schedules with cron or event-triggered execution';

--rollback DROP TABLE IF EXISTS "notification_schedules";
