--liquibase formatted sql

--changeset auth-service:20250121-001-add-platform-admin-to-membership-role-check
--comment: Extend the membership role CHECK constraint to include PLATFORM_ADMIN
ALTER TABLE tenant_memberships DROP CONSTRAINT IF EXISTS chk_membership_role;
ALTER TABLE tenant_memberships ADD CONSTRAINT chk_membership_role CHECK (role IN ('ADMIN', 'PMO', 'POC', 'PLATFORM_ADMIN'));
--rollback ALTER TABLE tenant_memberships DROP CONSTRAINT IF EXISTS chk_membership_role; ALTER TABLE tenant_memberships ADD CONSTRAINT chk_membership_role CHECK (role IN ('ADMIN', 'PMO', 'POC'));
