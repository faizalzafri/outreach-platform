--liquibase formatted sql

--changeset auth-service:20240102-001-seed-auth-users
--comment: Seed Spring Security users for development/testing
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM auth_users WHERE username = 'admin'
INSERT INTO auth_users (username, password, enabled)
VALUES
    ('admin',         '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', TRUE),
    ('priya_sharma',  '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', TRUE),
    ('vikram_singh',  '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', TRUE),
    ('anita_desai',   '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', TRUE),
    ('rahul_verma',   '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', TRUE);

--changeset auth-service:20240102-001-seed-auth-authorities
--comment: Seed Spring Security authorities (roles) for test users
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM auth_authorities WHERE username = 'admin'
INSERT INTO auth_authorities (username, authority)
VALUES
    ('admin',         'ROLE_ADMIN'),
    ('priya_sharma',  'ROLE_PMO'),
    ('vikram_singh',  'ROLE_PMO'),
    ('anita_desai',   'ROLE_POC'),
    ('rahul_verma',   'ROLE_POC');
