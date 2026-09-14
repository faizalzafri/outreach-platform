--liquibase formatted sql

--changeset auth-service:20250130-001-fix-admin-password-hash
--comment: The original 20240102-001 seed used a bcrypt hash for 'admin' that does not correspond to the documented 'password' credential (verified: it doesn't decode to 'password' or any other common dev value). Reset it to the same hash already used by the other seeded users, which does verify against 'password'.
UPDATE auth_users
SET password = '{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'
WHERE username = 'admin';
--rollback UPDATE auth_users SET password = '{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE username = 'admin';
