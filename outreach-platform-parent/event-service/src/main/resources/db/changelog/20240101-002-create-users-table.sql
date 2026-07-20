--liquibase formatted sql

--changeset outreach-platform:20240101-002-create-users-table
--comment: Create users table - manages platform user accounts (ADMIN, PMO, POC roles)
CREATE TABLE "users" (
    "id"                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    "username"                VARCHAR(100)    NOT NULL,
    "email_encrypted"         VARCHAR(255)    NOT NULL,
    "password_hash"           VARCHAR(255)    NOT NULL,
    "role"                    VARCHAR(20)     NOT NULL,
    "enabled"                 BOOLEAN         NOT NULL DEFAULT TRUE,
    "account_locked"          BOOLEAN         NOT NULL DEFAULT FALSE,
    "failed_login_attempts"   INTEGER         NOT NULL DEFAULT 0,
    "locked_until"            TIMESTAMP WITH TIME ZONE,
    "force_password_change"   BOOLEAN         NOT NULL DEFAULT TRUE,
    "last_login_at"           TIMESTAMP WITH TIME ZONE,
    "created_at"              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "updated_at"              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "created_by"              VARCHAR(100),
    "version"                 BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT "pk_users" PRIMARY KEY ("id"),
    CONSTRAINT "uk_users_username" UNIQUE ("username"),
    CONSTRAINT "chk_users_role" CHECK ("role" IN ('ADMIN', 'PMO', 'POC'))
);

COMMENT ON TABLE "users" IS 'Platform user accounts with role-based access control';
COMMENT ON COLUMN "users"."email_encrypted" IS 'AES-256 encrypted PII field';
COMMENT ON COLUMN "users"."version" IS 'Optimistic locking version counter';

--rollback DROP TABLE IF EXISTS "users";
