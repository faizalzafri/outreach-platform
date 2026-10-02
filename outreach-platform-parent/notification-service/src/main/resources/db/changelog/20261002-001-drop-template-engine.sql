--liquibase formatted sql

--changeset notification-service:20261002-001-drop-template-engine
--comment: Templates are always rendered with Thymeleaf; the engine column only ever said so.
ALTER TABLE "notification_templates" DROP COLUMN IF EXISTS "engine";
