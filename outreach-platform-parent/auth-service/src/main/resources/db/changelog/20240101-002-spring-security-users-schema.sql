--liquibase formatted sql

--changeset auth-service:002-users-table
--comment: Spring Security JDBC - users table for resource owner authentication
CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50)  NOT NULL,
    password VARCHAR(500) NOT NULL,
    enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (username)
);
--rollback DROP TABLE IF EXISTS users;

--changeset auth-service:002-authorities-table
--comment: Spring Security JDBC - user authorities/roles table
CREATE TABLE IF NOT EXISTS authorities (
    username  VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users(username),
    CONSTRAINT ix_auth_username UNIQUE (username, authority)
);
--rollback DROP TABLE IF EXISTS authorities;
