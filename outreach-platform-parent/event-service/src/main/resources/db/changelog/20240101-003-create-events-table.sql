--liquibase formatted sql

--changeset outreach-platform:20240101-003-create-events-table
--comment: Create events table - outreach event lifecycle management
CREATE TABLE "events" (
    "id"                UUID            NOT NULL DEFAULT gen_random_uuid(),
    "event_code"        VARCHAR(100)    NOT NULL,
    "event_name"        VARCHAR(255)    NOT NULL,
    "description"       TEXT,
    "status"            VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    "event_date"        DATE,
    "event_end_date"    DATE,
    "city"              VARCHAR(100),
    "venue"             VARCHAR(255),
    "category"          VARCHAR(50),
    "max_volunteers"    INTEGER,
    "registered_count"  INTEGER         NOT NULL DEFAULT 0,
    "attended_count"    INTEGER         NOT NULL DEFAULT 0,
    "published_at"      TIMESTAMP WITH TIME ZONE,
    "completed_at"      TIMESTAMP WITH TIME ZONE,
    "archived_at"       TIMESTAMP WITH TIME ZONE,
    "created_at"        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "updated_at"        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "created_by"        VARCHAR(100),
    "updated_by"        VARCHAR(100),
    "version"           BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT "pk_events" PRIMARY KEY ("id"),
    CONSTRAINT "uk_events_event_code" UNIQUE ("event_code"),
    CONSTRAINT "chk_events_status" CHECK ("status" IN ('DRAFT', 'PUBLISHED', 'ACTIVE', 'COMPLETED', 'ARCHIVED', 'CANCELLED')),
    CONSTRAINT "chk_events_date_range" CHECK ("event_end_date" IS NULL OR "event_end_date" >= "event_date")
);

COMMENT ON TABLE "events" IS 'Outreach events with lifecycle state machine (DRAFT→PUBLISHED→ACTIVE→COMPLETED→ARCHIVED)';
COMMENT ON COLUMN "events"."version" IS 'Optimistic locking version counter';

--rollback DROP TABLE IF EXISTS "events";
