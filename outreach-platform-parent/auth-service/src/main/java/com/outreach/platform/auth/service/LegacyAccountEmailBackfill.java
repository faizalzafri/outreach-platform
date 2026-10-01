package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.repo.UserAccountRepository;
import com.outreach.platform.common.pii.AesEncryptionConverter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Accounts created before auth-service stored contact details have no email. Event-service's user
 * table (same database, same PII key) holds it for the same username, so it is copied over once,
 * at startup. Accounts that already have an email, or have no matching row, are left alone.
 */
// ponytail: one-off bridge for databases created before the identity store; delete once every
// environment has started this version.
@Named
public class LegacyAccountEmailBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyAccountEmailBackfill.class);

    private final UserAccountRepository accounts;
    private final JdbcTemplate jdbcTemplate;
    private final AesEncryptionConverter pii = new AesEncryptionConverter();

    @Inject
    public LegacyAccountEmailBackfill(UserAccountRepository accounts, JdbcTemplate jdbcTemplate) {
        this.accounts = accounts;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // A database without event-service's tables (fresh environment, tests) has nothing to copy.
        Boolean usersTableExists = jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.users') IS NOT NULL", Boolean.class);
        if (!Boolean.TRUE.equals(usersTableExists)) {
            return;
        }
        int copied = 0;
        for (UserAccount account : accounts.findAll()) {
            if (account.getEmail() != null) {
                continue;
            }
            List<String> encrypted = jdbcTemplate.queryForList(
                    "SELECT email_encrypted FROM users WHERE username = ?", String.class, account.getUsername());
            if (!encrypted.isEmpty() && encrypted.getFirst() != null) {
                account.setEmail(pii.convertToEntityAttribute(encrypted.getFirst()));
                copied++;
            }
        }
        if (copied > 0) {
            log.info("Copied email addresses for {} existing accounts", copied);
        }
    }
}
