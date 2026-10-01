--liquibase formatted sql

--changeset auth-service:20261002-001-account-columns
--comment: auth_users becomes the single account store: stable UUID, contact details (PII, encrypted
--         by the application), lifecycle status, persistent lockout and password metadata.
ALTER TABLE auth_users ADD COLUMN id UUID;
ALTER TABLE auth_users ADD COLUMN email_encrypted VARCHAR(512);
ALTER TABLE auth_users ADD COLUMN email_hash VARCHAR(64);
ALTER TABLE auth_users ADD COLUMN display_name VARCHAR(100);
ALTER TABLE auth_users ADD COLUMN phone_encrypted VARCHAR(512);
ALTER TABLE auth_users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE auth_users ADD COLUMN platform_admin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE auth_users ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE auth_users ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE auth_users ADD COLUMN password_changed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE auth_users ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE auth_users ADD COLUMN created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();
ALTER TABLE auth_users ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'system';
ALTER TABLE auth_users ADD COLUMN last_modified_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE auth_users ADD COLUMN last_modified_by VARCHAR(255);
ALTER TABLE auth_users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE auth_users ALTER COLUMN password DROP NOT NULL;
ALTER TABLE auth_users ADD CONSTRAINT chk_auth_users_status CHECK (status IN ('INVITED', 'ACTIVE', 'DISABLED'));

--changeset auth-service:20261002-001-account-ids
--comment: The seeded accounts keep the ids event-service already uses for them (POC assignments and
--         team memberships reference those); any other account gets a fresh id. Display names are
--         derived from the username until the user edits their profile.
UPDATE auth_users SET id = CASE username
        WHEN 'admin'        THEN 'a0000000-0000-0000-0000-000000000001'::uuid
        WHEN 'priya_sharma' THEN 'a0000000-0000-0000-0000-000000000002'::uuid
        WHEN 'vikram_singh' THEN 'a0000000-0000-0000-0000-000000000003'::uuid
        WHEN 'anita_desai'  THEN 'a0000000-0000-0000-0000-000000000004'::uuid
        WHEN 'rahul_verma'  THEN 'a0000000-0000-0000-0000-000000000005'::uuid
        ELSE gen_random_uuid()
    END,
    display_name = initcap(replace(username, '_', ' ')),
    password_changed_at = NOW()
WHERE id IS NULL;

-- Memberships were keyed by Java's UUID.nameUUIDFromBytes(username); re-key them to the account id.
UPDATE tenant_memberships m
SET user_id = u.id
FROM auth_users u
CROSS JOIN LATERAL (SELECT md5(convert_to(u.username, 'UTF8')) AS h) md5_hash
WHERE m.user_id = (
        substring(h from 1 for 8) || '-' ||
        substring(h from 9 for 4) || '-' ||
        '3' || substring(h from 14 for 3) || '-' ||
        lpad(to_hex((get_byte(decode(substring(h from 17 for 2), 'hex'), 0) & x'3f'::int | x'80'::int)), 2, '0') ||
        substring(h from 19 for 2) || '-' ||
        substring(h from 21 for 12)
    )::uuid;

UPDATE auth_users SET platform_admin = TRUE
WHERE username IN (SELECT username FROM auth_authorities WHERE authority = 'ROLE_PLATFORM_ADMIN');

--changeset auth-service:20261002-001-account-keys
--comment: id becomes the primary key. Tenant roles now live only in tenant_memberships and the
--         platform-admin flag on the account, so the per-user authorities table goes away.
DROP TABLE auth_authorities;
ALTER TABLE auth_users DROP CONSTRAINT auth_users_pkey;
ALTER TABLE auth_users ALTER COLUMN id SET NOT NULL;
ALTER TABLE auth_users ADD CONSTRAINT pk_auth_users PRIMARY KEY (id);
ALTER TABLE auth_users ADD CONSTRAINT uq_auth_users_username UNIQUE (username);
CREATE UNIQUE INDEX uq_auth_users_email_hash ON auth_users (email_hash) WHERE email_hash IS NOT NULL;

--changeset auth-service:20261002-001-password-history
CREATE TABLE password_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES auth_users (id) ON DELETE CASCADE,
    password_hash       VARCHAR(500) NOT NULL,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255) NOT NULL,
    last_modified_date  TIMESTAMP WITH TIME ZONE,
    last_modified_by    VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_password_history_user ON password_history (user_id, created_date DESC);

--changeset auth-service:20261002-001-account-tokens
--comment: Single-use links for activation and password reset. Only the SHA-256 of the token is stored.
CREATE TABLE account_tokens (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES auth_users (id) ON DELETE CASCADE,
    purpose             VARCHAR(30) NOT NULL,
    token_hash          VARCHAR(64) NOT NULL UNIQUE,
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at             TIMESTAMP WITH TIME ZONE,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255) NOT NULL,
    last_modified_date  TIMESTAMP WITH TIME ZONE,
    last_modified_by    VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_account_tokens_purpose CHECK (purpose IN ('ACTIVATION', 'PASSWORD_RESET'))
);
CREATE INDEX idx_account_tokens_user ON account_tokens (user_id, purpose);

--changeset auth-service:20261002-001-otp-challenges
--comment: One-time passcodes; only the hash of the code is stored.
CREATE TABLE otp_challenges (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES auth_users (id) ON DELETE CASCADE,
    purpose             VARCHAR(30) NOT NULL,
    channel             VARCHAR(20) NOT NULL,
    code_hash           VARCHAR(64) NOT NULL,
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts            INTEGER NOT NULL DEFAULT 0,
    max_attempts        INTEGER NOT NULL,
    consumed_at         TIMESTAMP WITH TIME ZONE,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255) NOT NULL,
    last_modified_date  TIMESTAMP WITH TIME ZONE,
    last_modified_by    VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_otp_purpose CHECK (purpose IN ('LOGIN', 'PASSWORD_RESET', 'PASSWORD_CHANGE'))
);
CREATE INDEX idx_otp_challenges_user ON otp_challenges (user_id, purpose, created_date DESC);

--changeset auth-service:20261002-001-tenant-security-policy
--comment: Per-tenant password and OTP policy. A missing row means platform defaults apply.
CREATE TABLE tenant_security_policy (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL UNIQUE REFERENCES tenants (id) ON DELETE CASCADE,
    password_min_length     INTEGER,
    password_history_count  INTEGER,
    otp                     JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255) NOT NULL,
    last_modified_date      TIMESTAMP WITH TIME ZONE,
    last_modified_by        VARCHAR(255),
    version                 BIGINT NOT NULL DEFAULT 0
);

--changeset auth-service:20261002-001-auth-outbox
--comment: Transactional outbox: identity events commit with the account change and are published by a poller.
CREATE TABLE auth_outbox (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    routing_key         VARCHAR(100) NOT NULL,
    tenant_id           UUID NOT NULL,
    payload             JSONB NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    published_at        TIMESTAMP WITH TIME ZONE,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255) NOT NULL,
    last_modified_date  TIMESTAMP WITH TIME ZONE,
    last_modified_by    VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_auth_outbox_pending ON auth_outbox (created_date) WHERE status = 'PENDING';
