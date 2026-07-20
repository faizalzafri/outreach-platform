--liquibase formatted sql

--changeset outreach-platform:20240101-010-create-notification-templates-table
--comment: Create notification_templates table - reusable email/notification templates
CREATE TABLE "notification_templates" (
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

COMMENT ON TABLE "notification_templates" IS 'Reusable notification templates with variable schema definition';

--rollback DROP TABLE IF EXISTS "notification_templates";
