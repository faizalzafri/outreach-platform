--liquibase formatted sql

--changeset outreach-platform:20240101-001-create-volunteer-feedback-table
--comment: Create volunteer_feedback table for feedback-service integration tests (no FK to external tables)
CREATE TABLE "volunteer_feedback" (
    "id"            UUID            NOT NULL DEFAULT gen_random_uuid(),
    "tenant_id"     UUID            NOT NULL,
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
    "created_by"    VARCHAR(100)    NOT NULL DEFAULT 'system',
    "updated_by"    VARCHAR(100),
    "version"       BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT "pk_volunteer_feedback" PRIMARY KEY ("id"),
    CONSTRAINT "uk_volunteer_feedback_event_volunteer" UNIQUE ("event_id", "volunteer_id"),
    CONSTRAINT "chk_volunteer_feedback_score" CHECK ("score" >= 1 AND "score" <= 5),
    CONSTRAINT "chk_volunteer_feedback_sentiment" CHECK ("sentiment" IS NULL OR "sentiment" IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE')),
    CONSTRAINT "chk_volunteer_feedback_status" CHECK ("status" IN ('SUBMITTED', 'REVIEWED', 'FLAGGED', 'ARCHIVED'))
);

CREATE INDEX "idx_volunteer_feedback_event_id" ON "volunteer_feedback" ("event_id");
CREATE INDEX "idx_volunteer_feedback_volunteer_id" ON "volunteer_feedback" ("volunteer_id");
CREATE INDEX "idx_volunteer_feedback_score" ON "volunteer_feedback" ("score");
CREATE INDEX "idx_volunteer_feedback_category" ON "volunteer_feedback" ("category");
CREATE INDEX "idx_volunteer_feedback_sentiment" ON "volunteer_feedback" ("sentiment");
CREATE INDEX "idx_volunteer_feedback_status" ON "volunteer_feedback" ("status");
CREATE INDEX "idx_volunteer_feedback_submitted_at" ON "volunteer_feedback" ("submitted_at" DESC);

--rollback DROP TABLE IF EXISTS "volunteer_feedback";
