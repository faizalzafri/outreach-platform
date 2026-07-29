--liquibase formatted sql

--changeset auth-service:20250121-002-seed-default-tenant-memberships
--comment: Seed tenant_memberships for all existing auth_users → Default_Tenant with appropriate roles.
--         Uses deterministic UUID derivation from username matching Java's UUID.nameUUIDFromBytes(username.getBytes(UTF_8)).
--         Java's nameUUIDFromBytes computes MD5, then sets version=3 (nibble at position 13) and variant=10xx (byte 8 high bits).
--         Role mapping: ROLE_ADMIN → ADMIN, ROLE_PMO → PMO, ROLE_POC → POC.
--         Idempotent via ON CONFLICT DO NOTHING on the (tenant_id, user_id, role) unique constraint.

INSERT INTO tenant_memberships (id, tenant_id, user_id, role, created_date, created_by, version)
SELECT
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001'::uuid,
    -- Construct UUID v3 from MD5 of username (matching Java UUID.nameUUIDFromBytes)
    -- MD5 hex positions: 0-7 | 8-11 | 12-15 | 16-19 | 20-31
    -- Version 3: character at position 13 (0-indexed 12) becomes '3'
    -- Variant 10xx: character at position 17 (0-indexed 16) high bits set to 10xx
    (
        substring(h from 1 for 8) || '-' ||
        substring(h from 9 for 4) || '-' ||
        '3' || substring(h from 14 for 3) || '-' ||
        lpad(to_hex((get_byte(decode(substring(h from 17 for 2), 'hex'), 0) & x'3f'::int | x'80'::int)), 2, '0') ||
        substring(h from 19 for 2) || '-' ||
        substring(h from 21 for 12)
    )::uuid,
    CASE aa.authority
        WHEN 'ROLE_ADMIN' THEN 'ADMIN'
        WHEN 'ROLE_PMO' THEN 'PMO'
        WHEN 'ROLE_POC' THEN 'POC'
    END,
    NOW(),
    'system',
    0
FROM auth_users au
JOIN auth_authorities aa ON aa.username = au.username
CROSS JOIN LATERAL (SELECT md5(convert_to(au.username, 'UTF8')) AS h) md5_hash
WHERE aa.authority IN ('ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC')
  AND au.enabled = TRUE
ON CONFLICT ON CONSTRAINT uq_tenant_user_role DO NOTHING;
--rollback DELETE FROM tenant_memberships WHERE tenant_id = '00000000-0000-0000-0000-000000000001' AND created_by = 'system';
