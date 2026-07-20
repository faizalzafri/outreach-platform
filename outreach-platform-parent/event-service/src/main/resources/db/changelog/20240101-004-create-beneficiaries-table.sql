--liquibase formatted sql

--changeset outreach-platform:20240101-004-create-beneficiaries-table
--comment: Create beneficiaries table - organizations/entities receiving outreach services
CREATE TABLE "beneficiaries" (
    "id"                        UUID            NOT NULL DEFAULT gen_random_uuid(),
    "name"                      VARCHAR(255)    NOT NULL,
    "organization"              VARCHAR(255),
    "contact_email_encrypted"   VARCHAR(255),
    "contact_phone_encrypted"   VARCHAR(50),
    "city"                      VARCHAR(100),
    "address"                   VARCHAR(255),
    "description"               TEXT,
    "active"                    BOOLEAN         NOT NULL DEFAULT TRUE,
    "created_at"                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "updated_at"                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "version"                   BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT "pk_beneficiaries" PRIMARY KEY ("id")
);

COMMENT ON TABLE "beneficiaries" IS 'Organizations or entities receiving outreach services';
COMMENT ON COLUMN "beneficiaries"."contact_email_encrypted" IS 'AES-256 encrypted PII field';
COMMENT ON COLUMN "beneficiaries"."contact_phone_encrypted" IS 'AES-256 encrypted PII field';
COMMENT ON COLUMN "beneficiaries"."version" IS 'Optimistic locking version counter';

--rollback DROP TABLE IF EXISTS "beneficiaries";
