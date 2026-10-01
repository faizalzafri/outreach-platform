--liquibase formatted sql

--changeset event-service:20261002-001-users-become-directory
--comment: Accounts, passwords and lockout now live in auth-service. This table becomes a read-only
--         directory of users (kept in sync from auth-service's identity.user-changed events) that
--         POC assignment, teams and reports join against.
ALTER TABLE users DROP COLUMN password_hash;
ALTER TABLE users DROP COLUMN account_locked;
ALTER TABLE users DROP COLUMN failed_login_attempts;
ALTER TABLE users DROP COLUMN locked_until;
ALTER TABLE users DROP COLUMN force_password_change;
ALTER TABLE users DROP COLUMN last_login_at;
ALTER TABLE users ADD COLUMN display_name VARCHAR(100);
UPDATE users SET display_name = initcap(replace(username, '_', ' ')) WHERE display_name IS NULL;
