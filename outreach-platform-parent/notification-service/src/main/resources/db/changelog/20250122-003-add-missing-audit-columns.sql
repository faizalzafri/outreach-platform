--liquibase formatted sql

--changeset notification-service:20250122-003-add-audit-columns-to-notification-schedules
--comment: Adds columns needed to migrate NotificationScheduleEntity onto TenantAwareBaseEntity (docs/specs/platform-hardening/) - notification_schedules has created_at/created_by but no updated_at/updated_by/version
ALTER TABLE "notification_schedules" ADD COLUMN "updated_at" TIMESTAMP WITH TIME ZONE;
ALTER TABLE "notification_schedules" ADD COLUMN "updated_by" VARCHAR(100);
ALTER TABLE "notification_schedules" ADD COLUMN "version" BIGINT NOT NULL DEFAULT 0;
--rollback ALTER TABLE "notification_schedules" DROP COLUMN IF EXISTS "updated_at"; ALTER TABLE "notification_schedules" DROP COLUMN IF EXISTS "updated_by"; ALTER TABLE "notification_schedules" DROP COLUMN IF EXISTS "version";

--changeset notification-service:20250122-003-widen-notification-templates-version
--comment: BaseEntity's @Version field is Long-backed (BIGINT); notification_templates.version was INTEGER, needs widening before NotificationTemplateEntity can extend TenantAwareBaseEntity
ALTER TABLE "notification_templates" ALTER COLUMN "version" TYPE BIGINT;
--rollback ALTER TABLE "notification_templates" ALTER COLUMN "version" TYPE INTEGER;

--changeset notification-service:20250122-003-add-updated-by-to-notification-templates
--comment: notification_templates has created_at/updated_at/created_by but no updated_by
ALTER TABLE "notification_templates" ADD COLUMN "updated_by" VARCHAR(100);
--rollback ALTER TABLE "notification_templates" DROP COLUMN IF EXISTS "updated_by";
