--liquibase formatted sql

--changeset outreach-platform:20240101-006-create-volunteers-table
--comment: Create volunteers table - employee volunteer profiles with PII encryption
CREATE TABLE "volunteers" (
    "id"                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    "employee_id"                 VARCHAR(50)     NOT NULL,
    "full_name_encrypted"         VARCHAR(255)    NOT NULL,
    "email_encrypted"             VARCHAR(255)    NOT NULL,
    "phone_encrypted"             VARCHAR(50),
    "base_location"               VARCHAR(100),
    "department"                  VARCHAR(100),
    "designation"                 VARCHAR(50),
    "skills"                      TEXT,
    "availability"                VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    "total_events_participated"   INTEGER         NOT NULL DEFAULT 0,
    "avg_feedback_score"          DECIMAL(3,2),
    "last_participated_at"        TIMESTAMP WITH TIME ZONE,
    "created_at"                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "updated_at"                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "version"                     BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT "pk_volunteers" PRIMARY KEY ("id"),
    CONSTRAINT "uk_volunteers_employee_id" UNIQUE ("employee_id"),
    CONSTRAINT "chk_volunteers_availability" CHECK ("availability" IN ('AVAILABLE', 'BUSY', 'ON_LEAVE'))
);

COMMENT ON TABLE "volunteers" IS 'Employee volunteer profiles with participation tracking';
COMMENT ON COLUMN "volunteers"."full_name_encrypted" IS 'AES-256 encrypted PII field';
COMMENT ON COLUMN "volunteers"."email_encrypted" IS 'AES-256 encrypted PII field';
COMMENT ON COLUMN "volunteers"."phone_encrypted" IS 'AES-256 encrypted PII field';
COMMENT ON COLUMN "volunteers"."skills" IS 'Comma-separated skill tags';
COMMENT ON COLUMN "volunteers"."version" IS 'Optimistic locking version counter';

--rollback DROP TABLE IF EXISTS "volunteers";
