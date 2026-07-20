--liquibase formatted sql

--changeset outreach-platform:20240101-007-create-event-enrollment-table
--comment: Create event_enrollment table - tracks volunteer registration and attendance per event
CREATE TABLE "event_enrollment" (
    "id"                    UUID            NOT NULL DEFAULT gen_random_uuid(),
    "event_id"              UUID            NOT NULL,
    "volunteer_id"          UUID            NOT NULL,
    "attendance_status"     VARCHAR(20)     NOT NULL DEFAULT 'REGISTERED',
    "email_status"          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    "registered_at"         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "attendance_marked_at"  TIMESTAMP WITH TIME ZONE,
    "marked_by"             VARCHAR(100),
    "created_at"            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT "pk_event_enrollment" PRIMARY KEY ("id"),
    CONSTRAINT "fk_event_enrollment_event" FOREIGN KEY ("event_id")
        REFERENCES "events" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_event_enrollment_volunteer" FOREIGN KEY ("volunteer_id")
        REFERENCES "volunteers" ("id") ON DELETE CASCADE,
    CONSTRAINT "uk_event_enrollment_event_volunteer" UNIQUE ("event_id", "volunteer_id"),
    CONSTRAINT "chk_event_enrollment_attendance" CHECK ("attendance_status" IN ('REGISTERED', 'ATTENDED', 'NOT_ATTENDED', 'UNREGISTERED')),
    CONSTRAINT "chk_event_enrollment_email" CHECK ("email_status" IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'BOUNCED'))
);

COMMENT ON TABLE "event_enrollment" IS 'Volunteer enrollment and attendance tracking per event';

--rollback DROP TABLE IF EXISTS "event_enrollment";
