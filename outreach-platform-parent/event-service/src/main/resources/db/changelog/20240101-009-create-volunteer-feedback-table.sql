--liquibase formatted sql

--changeset outreach-platform:20240101-009-create-volunteer-feedback-table
--comment: Create volunteer_feedback table - feedback submissions from volunteers per event
CREATE TABLE "volunteer_feedback" (
    "id"            UUID            NOT NULL DEFAULT gen_random_uuid(),
    "event_id"      UUID            NOT NULL,
    "volunteer_id"  UUID            NOT NULL,
    "score"         INTEGER         NOT NULL,
    "answer1"       TEXT,
    "answer2"       TEXT,
    "answer3"       TEXT,
    "category"      VARCHAR(50),
    "tags"          VARCHAR(255),
    "sentiment"     VARCHAR(20),
    "status"        VARCHAR(20)     NOT NULL DEFAULT 'SUBMITTED',
    "anonymous"     BOOLEAN         NOT NULL DEFAULT FALSE,
    "submitted_at"  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "reviewed_at"   TIMESTAMP WITH TIME ZONE,
    "reviewed_by"   VARCHAR(100),
    "created_at"    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "updated_at"    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "version"       BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT "pk_volunteer_feedback" PRIMARY KEY ("id"),
    CONSTRAINT "fk_volunteer_feedback_event" FOREIGN KEY ("event_id")
        REFERENCES "events" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_volunteer_feedback_volunteer" FOREIGN KEY ("volunteer_id")
        REFERENCES "volunteers" ("id") ON DELETE CASCADE,
    CONSTRAINT "uk_volunteer_feedback_event_volunteer" UNIQUE ("event_id", "volunteer_id"),
    CONSTRAINT "chk_volunteer_feedback_score" CHECK ("score" >= 1 AND "score" <= 5),
    CONSTRAINT "chk_volunteer_feedback_sentiment" CHECK ("sentiment" IS NULL OR "sentiment" IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE')),
    CONSTRAINT "chk_volunteer_feedback_status" CHECK ("status" IN ('SUBMITTED', 'REVIEWED', 'FLAGGED', 'ARCHIVED'))
);

COMMENT ON TABLE "volunteer_feedback" IS 'Volunteer feedback submissions with scoring and sentiment analysis';
COMMENT ON COLUMN "volunteer_feedback"."score" IS 'Rating 1-5 scale';
COMMENT ON COLUMN "volunteer_feedback"."tags" IS 'Comma-separated tag values';
COMMENT ON COLUMN "volunteer_feedback"."version" IS 'Optimistic locking version counter';

--rollback DROP TABLE IF EXISTS "volunteer_feedback";
