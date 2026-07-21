--liquibase formatted sql

--changeset auth-service:002-auth-users-table
--comment: Spring Security JDBC - auth_users table for resource owner authentication (separate from platform users table)
CREATE TABLE IF NOT EXISTS auth_users (
    username VARCHAR(50)  NOT NULL,
    password VARCHAR(500) NOT NULL,
    enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (username)
);
--rollback DROP TABLE IF EXISTS auth_users;

--changeset auth-service:002-auth-authorities-table
--comment: Spring Security JDBC - auth_authorities/roles table
CREATE TABLE IF NOT EXISTS auth_authorities (
    username  VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_auth_authorities_users FOREIGN KEY (username) REFERENCES auth_users(username),
    CONSTRAINT ix_auth_authorities_username UNIQUE (username, authority)
);
--rollback DROP TABLE IF EXISTS auth_authorities;
