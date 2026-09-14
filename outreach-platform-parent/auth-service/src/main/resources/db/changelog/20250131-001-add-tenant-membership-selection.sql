--liquibase formatted sql

--changeset auth-service:20250131-001-add-last-selected-at-to-tenant-memberships
--comment: Tracks which tenant a user with multiple tenant memberships last explicitly chose, so token issuance stops arbitrarily picking the first active membership and instead honors the user's actual selection.
ALTER TABLE tenant_memberships ADD COLUMN last_selected_at TIMESTAMP WITH TIME ZONE;
--rollback ALTER TABLE tenant_memberships DROP COLUMN last_selected_at;
