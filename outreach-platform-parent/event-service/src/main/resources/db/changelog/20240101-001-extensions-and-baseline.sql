--liquibase formatted sql

--changeset outreach-platform:20240101-001-extensions-and-baseline
--comment: Enable required PostgreSQL extensions and establish baseline from MySQL migration
-- MySQL→PostgreSQL migration baseline:
--   TINYINT(1) → BOOLEAN
--   AUTO_INCREMENT → GENERATED ALWAYS AS IDENTITY
--   backtick identifiers → double-quoted identifiers
--   DATETIME → TIMESTAMP WITH TIME ZONE
--   gen_random_uuid() for UUID defaults (PostgreSQL 16+)

-- Enable pgcrypto for gen_random_uuid() (built-in from PG 13+, but explicit for clarity)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Enable pg_trgm for full-text/trigram search
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

--rollback DROP EXTENSION IF EXISTS "pg_trgm";
--rollback DROP EXTENSION IF EXISTS "pgcrypto";
