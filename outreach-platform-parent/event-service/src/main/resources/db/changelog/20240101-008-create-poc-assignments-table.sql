--liquibase formatted sql

--changeset outreach-platform:20240101-008-create-poc-assignments-table
--comment: Create poc_assignments table - Point of Contact assignments to events
CREATE TABLE "poc_assignments" (
    "id"                UUID            NOT NULL DEFAULT gen_random_uuid(),
    "event_id"          UUID            NOT NULL,
    "user_id"           UUID            NOT NULL,
    "assignment_role"   VARCHAR(50)     NOT NULL DEFAULT 'PRIMARY',
    "assigned_at"       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "assigned_by"       VARCHAR(100),

    CONSTRAINT "pk_poc_assignments" PRIMARY KEY ("id"),
    CONSTRAINT "fk_poc_assignments_event" FOREIGN KEY ("event_id")
        REFERENCES "events" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_poc_assignments_user" FOREIGN KEY ("user_id")
        REFERENCES "users" ("id") ON DELETE CASCADE,
    CONSTRAINT "uk_poc_assignments_event_user" UNIQUE ("event_id", "user_id"),
    CONSTRAINT "chk_poc_assignments_role" CHECK ("assignment_role" IN ('PRIMARY', 'SECONDARY'))
);

COMMENT ON TABLE "poc_assignments" IS 'Point of Contact (POC) user assignments to outreach events';

--rollback DROP TABLE IF EXISTS "poc_assignments";
