package com.outreach.platform.auth.service;

import com.outreach.platform.auth.config.AuthServiceProperties;
import com.outreach.platform.auth.repo.UserAccountRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Brute-force protection: after {@code maxFailedAttempts} consecutive failures the account is
 * locked for {@code lockDuration}. State lives on the account row, so it survives restarts and is
 * shared by every auth-service instance. Unknown usernames are ignored.
 */
@Named
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class AccountLockoutService {

    private static final Logger log = LoggerFactory.getLogger(AccountLockoutService.class);

    private final UserAccountRepository accounts;
    private final int maxFailedAttempts;
    private final Duration lockDuration;

    @Inject
    public AccountLockoutService(UserAccountRepository accounts, AuthServiceProperties properties) {
        this.accounts = accounts;
        var bruteForce = properties.security().bruteForce();
        this.maxFailedAttempts = bruteForce.maxFailedAttempts();
        this.lockDuration = bruteForce.lockDuration();
    }

    @Transactional
    public void recordFailedAttempt(String username) {
        Instant now = Instant.now();
        accounts.findByUsername(username).ifPresent(account -> {
            // A lock that has run out starts a fresh count rather than relocking on the next miss.
            if (account.getLockedUntil() != null && !account.isLocked(now)) {
                account.setLockedUntil(null);
                account.setFailedLoginAttempts(0);
            }
            int attempts = account.getFailedLoginAttempts() + 1;
            account.setFailedLoginAttempts(attempts);
            if (attempts >= maxFailedAttempts && account.getLockedUntil() == null) {
                account.setLockedUntil(now.plus(lockDuration));
                log.warn("Account locked after {} failed sign-ins: userId={}", attempts, account.getId());
            }
        });
    }

    @Transactional(readOnly = true)
    public boolean isLocked(String username) {
        return accounts.findByUsername(username).map(a -> a.isLocked(Instant.now())).orElse(false);
    }

    /** Successful sign-in: clears failures and records the time. */
    @Transactional
    public void recordSuccessfulLogin(String username) {
        accounts.findByUsername(username).ifPresent(account -> {
            account.setFailedLoginAttempts(0);
            account.setLockedUntil(null);
            account.setLastLoginAt(Instant.now());
        });
    }

    @Transactional(readOnly = true)
    public int getFailedAttempts(String username) {
        return accounts.findByUsername(username).map(a -> a.getFailedLoginAttempts()).orElse(0);
    }
}
