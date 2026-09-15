--liquibase formatted sql

--changeset event-service:20250122-004-add-updated-by-to-volunteers
--comment: Adds columns needed to migrate hand-rolled tenant-filter entities onto TenantAwareBaseEntity (docs/specs/platform-hardening/) - volunteers has created_at/updated_at/version but no created_by/updated_by
ALTER TABLE "volunteers" ADD COLUMN "created_by" VARCHAR(100) NOT NULL DEFAULT 'system';
ALTER TABLE "volunteers" ADD COLUMN "updated_by" VARCHAR(100);
--rollback ALTER TABLE "volunteers" DROP COLUMN IF EXISTS "created_by"; ALTER TABLE "volunteers" DROP COLUMN IF EXISTS "updated_by";

--changeset event-service:20250122-004-add-updated-by-to-beneficiaries
--comment: beneficiaries has created_at/updated_at/version but no created_by/updated_by
ALTER TABLE "beneficiaries" ADD COLUMN "created_by" VARCHAR(100) NOT NULL DEFAULT 'system';
ALTER TABLE "beneficiaries" ADD COLUMN "updated_by" VARCHAR(100);
--rollback ALTER TABLE "beneficiaries" DROP COLUMN IF EXISTS "created_by"; ALTER TABLE "beneficiaries" DROP COLUMN IF EXISTS "updated_by";

--changeset event-service:20250122-004-add-updated-by-to-volunteer-feedback
--comment: volunteer_feedback has created_at/updated_at/version but no created_by/updated_by
ALTER TABLE "volunteer_feedback" ADD COLUMN "created_by" VARCHAR(100) NOT NULL DEFAULT 'system';
ALTER TABLE "volunteer_feedback" ADD COLUMN "updated_by" VARCHAR(100);
--rollback ALTER TABLE "volunteer_feedback" DROP COLUMN IF EXISTS "created_by"; ALTER TABLE "volunteer_feedback" DROP COLUMN IF EXISTS "updated_by";

--changeset event-service:20250122-004-add-updated-by-to-users
--comment: users has created_at/updated_at/created_by/version but no updated_by
ALTER TABLE "users" ADD COLUMN "updated_by" VARCHAR(100);
--rollback ALTER TABLE "users" DROP COLUMN IF EXISTS "updated_by";

--changeset event-service:20250122-004-add-audit-columns-to-event-enrollment
--comment: event_enrollment has only created_at — add updated_at/created_by/updated_by/version
ALTER TABLE "event_enrollment" ADD COLUMN "updated_at" TIMESTAMP WITH TIME ZONE;
ALTER TABLE "event_enrollment" ADD COLUMN "created_by" VARCHAR(100) NOT NULL DEFAULT 'system';
ALTER TABLE "event_enrollment" ADD COLUMN "updated_by" VARCHAR(100);
ALTER TABLE "event_enrollment" ADD COLUMN "version" BIGINT NOT NULL DEFAULT 0;
--rollback ALTER TABLE "event_enrollment" DROP COLUMN IF EXISTS "updated_at"; ALTER TABLE "event_enrollment" DROP COLUMN IF EXISTS "created_by"; ALTER TABLE "event_enrollment" DROP COLUMN IF EXISTS "updated_by"; ALTER TABLE "event_enrollment" DROP COLUMN IF EXISTS "version";

--changeset event-service:20250122-004-add-audit-columns-to-poc-assignments
--comment: poc_assignments has assigned_at/assigned_by (a distinct domain concept) instead of generic audit columns; add generic created_at/created_by/updated_at/updated_by/version alongside them per docs/specs/platform-hardening/design.md
ALTER TABLE "poc_assignments" ADD COLUMN "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();
ALTER TABLE "poc_assignments" ADD COLUMN "updated_at" TIMESTAMP WITH TIME ZONE;
ALTER TABLE "poc_assignments" ADD COLUMN "created_by" VARCHAR(100) NOT NULL DEFAULT 'system';
ALTER TABLE "poc_assignments" ADD COLUMN "updated_by" VARCHAR(100);
ALTER TABLE "poc_assignments" ADD COLUMN "version" BIGINT NOT NULL DEFAULT 0;
--rollback ALTER TABLE "poc_assignments" DROP COLUMN IF EXISTS "created_at"; ALTER TABLE "poc_assignments" DROP COLUMN IF EXISTS "updated_at"; ALTER TABLE "poc_assignments" DROP COLUMN IF EXISTS "created_by"; ALTER TABLE "poc_assignments" DROP COLUMN IF EXISTS "updated_by"; ALTER TABLE "poc_assignments" DROP COLUMN IF EXISTS "version";
