--liquibase formatted sql

--changeset event-service:20250122-003-add-tenant-id-to-team-memberships
--comment: team_memberships was missing tenant_id in the original 20250122-001 migration, even though TeamMembership extends TenantAwareBaseEntity
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'team_memberships' AND column_name = 'tenant_id'
ALTER TABLE team_memberships
    ADD COLUMN tenant_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';
--rollback ALTER TABLE team_memberships DROP COLUMN IF EXISTS tenant_id;

--changeset event-service:20250122-003-add-fk-team-memberships-tenant
--comment: Add foreign key constraint from team_memberships.tenant_id to tenants table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name = 'team_memberships' AND constraint_name = 'fk_team_memberships_tenant'
ALTER TABLE team_memberships
    ADD CONSTRAINT fk_team_memberships_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id);
--rollback ALTER TABLE team_memberships DROP CONSTRAINT IF EXISTS fk_team_memberships_tenant;

--changeset event-service:20250122-003-add-idx-team-memberships-tenant
--comment: Add index on tenant_id for tenant-scoped queries
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'team_memberships' AND indexname = 'idx_team_memberships_tenant'
CREATE INDEX idx_team_memberships_tenant ON team_memberships(tenant_id);
--rollback DROP INDEX IF EXISTS idx_team_memberships_tenant;

--changeset event-service:20250122-003-drop-default-team-memberships-tenant
--comment: Remove DEFAULT after backfilling existing rows
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'team_memberships' AND column_name = 'tenant_id' AND column_default IS NOT NULL
ALTER TABLE team_memberships ALTER COLUMN tenant_id DROP DEFAULT;
--rollback ALTER TABLE team_memberships ALTER COLUMN tenant_id SET DEFAULT '00000000-0000-0000-0000-000000000001';
