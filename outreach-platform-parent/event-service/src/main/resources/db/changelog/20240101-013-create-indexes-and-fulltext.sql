--liquibase formatted sql

--changeset outreach-platform:20240101-013-create-indexes-and-fulltext
--comment: Create performance indexes and pg_trgm full-text search indexes

-- =============================================
-- USERS indexes
-- =============================================
CREATE INDEX "idx_users_role" ON "users" ("role");
CREATE INDEX "idx_users_enabled" ON "users" ("enabled");
CREATE INDEX "idx_users_account_locked" ON "users" ("account_locked");

-- =============================================
-- EVENTS indexes
-- =============================================
CREATE INDEX "idx_events_status" ON "events" ("status");
CREATE INDEX "idx_events_event_date" ON "events" ("event_date");
CREATE INDEX "idx_events_city" ON "events" ("city");
CREATE INDEX "idx_events_category" ON "events" ("category");
CREATE INDEX "idx_events_created_by" ON "events" ("created_by");
CREATE INDEX "idx_events_created_at" ON "events" ("created_at" DESC);

-- Full-text search: trigram indexes for event name and description
CREATE INDEX "idx_events_event_name_trgm" ON "events" USING GIN ("event_name" gin_trgm_ops);
CREATE INDEX "idx_events_description_trgm" ON "events" USING GIN ("description" gin_trgm_ops);
CREATE INDEX "idx_events_city_trgm" ON "events" USING GIN ("city" gin_trgm_ops);

-- =============================================
-- BENEFICIARIES indexes
-- =============================================
CREATE INDEX "idx_beneficiaries_city" ON "beneficiaries" ("city");
CREATE INDEX "idx_beneficiaries_active" ON "beneficiaries" ("active");
CREATE INDEX "idx_beneficiaries_name_trgm" ON "beneficiaries" USING GIN ("name" gin_trgm_ops);
CREATE INDEX "idx_beneficiaries_organization_trgm" ON "beneficiaries" USING GIN ("organization" gin_trgm_ops);

-- =============================================
-- EVENT_BENEFICIARY indexes
-- =============================================
CREATE INDEX "idx_event_beneficiary_beneficiary_id" ON "event_beneficiary" ("beneficiary_id");

-- =============================================
-- VOLUNTEERS indexes
-- =============================================
CREATE INDEX "idx_volunteers_base_location" ON "volunteers" ("base_location");
CREATE INDEX "idx_volunteers_department" ON "volunteers" ("department");
CREATE INDEX "idx_volunteers_availability" ON "volunteers" ("availability");
CREATE INDEX "idx_volunteers_total_events" ON "volunteers" ("total_events_participated" DESC);
CREATE INDEX "idx_volunteers_avg_score" ON "volunteers" ("avg_feedback_score" DESC NULLS LAST);
CREATE INDEX "idx_volunteers_last_participated" ON "volunteers" ("last_participated_at" DESC NULLS LAST);

-- Full-text search: trigram indexes for volunteer skills and location
CREATE INDEX "idx_volunteers_skills_trgm" ON "volunteers" USING GIN ("skills" gin_trgm_ops);
CREATE INDEX "idx_volunteers_base_location_trgm" ON "volunteers" USING GIN ("base_location" gin_trgm_ops);

-- =============================================
-- EVENT_ENROLLMENT indexes
-- =============================================
CREATE INDEX "idx_event_enrollment_event_id" ON "event_enrollment" ("event_id");
CREATE INDEX "idx_event_enrollment_volunteer_id" ON "event_enrollment" ("volunteer_id");
CREATE INDEX "idx_event_enrollment_attendance" ON "event_enrollment" ("attendance_status");
CREATE INDEX "idx_event_enrollment_email_status" ON "event_enrollment" ("email_status");
CREATE INDEX "idx_event_enrollment_registered_at" ON "event_enrollment" ("registered_at" DESC);

-- =============================================
-- POC_ASSIGNMENTS indexes
-- =============================================
CREATE INDEX "idx_poc_assignments_event_id" ON "poc_assignments" ("event_id");
CREATE INDEX "idx_poc_assignments_user_id" ON "poc_assignments" ("user_id");

-- =============================================
-- VOLUNTEER_FEEDBACK indexes
-- =============================================
CREATE INDEX "idx_volunteer_feedback_event_id" ON "volunteer_feedback" ("event_id");
CREATE INDEX "idx_volunteer_feedback_volunteer_id" ON "volunteer_feedback" ("volunteer_id");
CREATE INDEX "idx_volunteer_feedback_score" ON "volunteer_feedback" ("score");
CREATE INDEX "idx_volunteer_feedback_category" ON "volunteer_feedback" ("category");
CREATE INDEX "idx_volunteer_feedback_sentiment" ON "volunteer_feedback" ("sentiment");
CREATE INDEX "idx_volunteer_feedback_status" ON "volunteer_feedback" ("status");
CREATE INDEX "idx_volunteer_feedback_submitted_at" ON "volunteer_feedback" ("submitted_at" DESC);

-- =============================================
-- NOTIFICATION_TEMPLATES indexes
-- =============================================
CREATE INDEX "idx_notification_templates_type" ON "notification_templates" ("type");
CREATE INDEX "idx_notification_templates_active" ON "notification_templates" ("active");

-- =============================================
-- NOTIFICATION_SCHEDULES indexes
-- =============================================
CREATE INDEX "idx_notification_schedules_template_id" ON "notification_schedules" ("template_id");
CREATE INDEX "idx_notification_schedules_event_id" ON "notification_schedules" ("event_id");
CREATE INDEX "idx_notification_schedules_status" ON "notification_schedules" ("status");
CREATE INDEX "idx_notification_schedules_trigger_type" ON "notification_schedules" ("trigger_type");
CREATE INDEX "idx_notification_schedules_scheduled_at" ON "notification_schedules" ("scheduled_at");

-- =============================================
-- REPORT_SCHEDULES indexes
-- =============================================
CREATE INDEX "idx_report_schedules_status" ON "report_schedules" ("status");
CREATE INDEX "idx_report_schedules_report_type" ON "report_schedules" ("report_type");
CREATE INDEX "idx_report_schedules_next_run_at" ON "report_schedules" ("next_run_at");

--rollback DROP INDEX IF EXISTS "idx_report_schedules_next_run_at";
--rollback DROP INDEX IF EXISTS "idx_report_schedules_report_type";
--rollback DROP INDEX IF EXISTS "idx_report_schedules_status";
--rollback DROP INDEX IF EXISTS "idx_notification_schedules_scheduled_at";
--rollback DROP INDEX IF EXISTS "idx_notification_schedules_trigger_type";
--rollback DROP INDEX IF EXISTS "idx_notification_schedules_status";
--rollback DROP INDEX IF EXISTS "idx_notification_schedules_event_id";
--rollback DROP INDEX IF EXISTS "idx_notification_schedules_template_id";
--rollback DROP INDEX IF EXISTS "idx_notification_templates_active";
--rollback DROP INDEX IF EXISTS "idx_notification_templates_type";
--rollback DROP INDEX IF EXISTS "idx_volunteer_feedback_submitted_at";
--rollback DROP INDEX IF EXISTS "idx_volunteer_feedback_status";
--rollback DROP INDEX IF EXISTS "idx_volunteer_feedback_sentiment";
--rollback DROP INDEX IF EXISTS "idx_volunteer_feedback_category";
--rollback DROP INDEX IF EXISTS "idx_volunteer_feedback_score";
--rollback DROP INDEX IF EXISTS "idx_volunteer_feedback_volunteer_id";
--rollback DROP INDEX IF EXISTS "idx_volunteer_feedback_event_id";
--rollback DROP INDEX IF EXISTS "idx_poc_assignments_user_id";
--rollback DROP INDEX IF EXISTS "idx_poc_assignments_event_id";
--rollback DROP INDEX IF EXISTS "idx_event_enrollment_registered_at";
--rollback DROP INDEX IF EXISTS "idx_event_enrollment_email_status";
--rollback DROP INDEX IF EXISTS "idx_event_enrollment_attendance";
--rollback DROP INDEX IF EXISTS "idx_event_enrollment_volunteer_id";
--rollback DROP INDEX IF EXISTS "idx_event_enrollment_event_id";
--rollback DROP INDEX IF EXISTS "idx_volunteers_base_location_trgm";
--rollback DROP INDEX IF EXISTS "idx_volunteers_skills_trgm";
--rollback DROP INDEX IF EXISTS "idx_volunteers_last_participated";
--rollback DROP INDEX IF EXISTS "idx_volunteers_avg_score";
--rollback DROP INDEX IF EXISTS "idx_volunteers_total_events";
--rollback DROP INDEX IF EXISTS "idx_volunteers_availability";
--rollback DROP INDEX IF EXISTS "idx_volunteers_department";
--rollback DROP INDEX IF EXISTS "idx_volunteers_base_location";
--rollback DROP INDEX IF EXISTS "idx_event_beneficiary_beneficiary_id";
--rollback DROP INDEX IF EXISTS "idx_beneficiaries_organization_trgm";
--rollback DROP INDEX IF EXISTS "idx_beneficiaries_name_trgm";
--rollback DROP INDEX IF EXISTS "idx_beneficiaries_active";
--rollback DROP INDEX IF EXISTS "idx_beneficiaries_city";
--rollback DROP INDEX IF EXISTS "idx_events_city_trgm";
--rollback DROP INDEX IF EXISTS "idx_events_description_trgm";
--rollback DROP INDEX IF EXISTS "idx_events_event_name_trgm";
--rollback DROP INDEX IF EXISTS "idx_events_created_at";
--rollback DROP INDEX IF EXISTS "idx_events_created_by";
--rollback DROP INDEX IF EXISTS "idx_events_category";
--rollback DROP INDEX IF EXISTS "idx_events_city";
--rollback DROP INDEX IF EXISTS "idx_events_event_date";
--rollback DROP INDEX IF EXISTS "idx_events_status";
--rollback DROP INDEX IF EXISTS "idx_users_account_locked";
--rollback DROP INDEX IF EXISTS "idx_users_enabled";
--rollback DROP INDEX IF EXISTS "idx_users_role";
