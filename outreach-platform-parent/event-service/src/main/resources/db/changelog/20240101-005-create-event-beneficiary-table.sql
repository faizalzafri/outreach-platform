--liquibase formatted sql

--changeset outreach-platform:20240101-005-create-event-beneficiary-table
--comment: Create event_beneficiary junction table - many-to-many between events and beneficiaries
CREATE TABLE "event_beneficiary" (
    "event_id"          UUID NOT NULL,
    "beneficiary_id"    UUID NOT NULL,

    CONSTRAINT "pk_event_beneficiary" PRIMARY KEY ("event_id", "beneficiary_id"),
    CONSTRAINT "fk_event_beneficiary_event" FOREIGN KEY ("event_id")
        REFERENCES "events" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_event_beneficiary_beneficiary" FOREIGN KEY ("beneficiary_id")
        REFERENCES "beneficiaries" ("id") ON DELETE CASCADE
);

COMMENT ON TABLE "event_beneficiary" IS 'Many-to-many relationship between events and beneficiaries';

--rollback DROP TABLE IF EXISTS "event_beneficiary";
