--liquibase formatted sql

--changeset feedback-service:20250120-003-add-created-by-to-volunteer-feedback
--comment: Add created_by column to volunteer_feedback for JPA auditing (BaseEntity.createdBy) - never created by event-service's original 009 migration
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'volunteer_feedback' AND column_name = 'created_by'
ALTER TABLE volunteer_feedback
    ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'system';
--rollback ALTER TABLE volunteer_feedback DROP COLUMN IF EXISTS created_by;

--changeset feedback-service:20250120-003-drop-default-volunteer-feedback-created-by
--comment: Remove DEFAULT after backfilling existing rows
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'volunteer_feedback' AND column_name = 'created_by' AND column_default IS NOT NULL
ALTER TABLE volunteer_feedback ALTER COLUMN created_by DROP DEFAULT;
--rollback ALTER TABLE volunteer_feedback ALTER COLUMN created_by SET DEFAULT 'system';

--changeset feedback-service:20250120-003-add-updated-by-to-volunteer-feedback
--comment: Add updated_by column to volunteer_feedback for JPA auditing (BaseEntity.lastModifiedBy, mapped via @AttributeOverride)
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'volunteer_feedback' AND column_name = 'updated_by'
ALTER TABLE volunteer_feedback
    ADD COLUMN updated_by VARCHAR(255);
--rollback ALTER TABLE volunteer_feedback DROP COLUMN IF EXISTS updated_by;
