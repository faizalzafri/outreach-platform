package com.outreach.platform.auth.service;

import com.outreach.platform.auth.config.AuthServiceProperties;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Brute-force protection service implementing account lockout after repeated failed attempts.
 * Configuration: 5 failed attempts → 30 minute lock (matching Keycloak realm settings).
 *
 * <p>This implementation uses an in-memory store. For clustered deployments,
 * replace with a Redis-backed implementation.</p>
 *
 * <p>Only active when {@code idp.provider=spring}. When Keycloak is the active provider,
 * lockout enforcement is handled by the Keycloak realm brute-force detection settings.
 */
@Named
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class AccountLockoutService {

    private static final Logger log = LoggerFactory.getLogger(AccountLockoutService.class);

    private final int maxFailedAttempts;
    private final long lockDurationMillis;

    private final ConcurrentMap<String, LockoutState> lockoutStates = new ConcurrentHashMap<>();

    @Inject
    public AccountLockoutService(AuthServiceProperties properties) {
        var bruteForce = properties.security().bruteForce();
        this.maxFailedAttempts = bruteForce.maxFailedAttempts();
        this.lockDurationMillis = bruteForce.lockDuration().toMillis();
    }

    /**
     * Records a failed authentication attempt for the given username.
     *
     * @param username the account identifier
     */
    public void recordFailedAttempt(String username) {
        lockoutStates.compute(username, (key, state) -> {
            if (state == null) {
                return new LockoutState(1, null);
            }
            int newCount = state.failedAttempts() + 1;
            Instant lockedUntil = state.lockedUntil();
            if (newCount >= maxFailedAttempts) {
                lockedUntil = Instant.now().plusMillis(lockDurationMillis);
                log.warn("Account locked: username={}, lockedUntil={}", username, lockedUntil);
            }
            return new LockoutState(newCount, lockedUntil);
        });
    }

    /**
     * Checks whether the account is currently locked out.
     *
     * @param username the account identifier
     * @return true if the account is locked and the lock duration has not elapsed
     */
    public boolean isLocked(String username) {
        LockoutState state = lockoutStates.get(username);
        if (state == null || state.lockedUntil() == null) {
            return false;
        }
        if (Instant.now().isAfter(state.lockedUntil())) {
            // Lock expired — reset state
            lockoutStates.remove(username);
            return false;
        }
        return true;
    }

    /**
     * Resets the lockout state upon successful authentication.
     *
     * @param username the account identifier
     */
    public void resetAttempts(String username) {
        lockoutStates.remove(username);
    }

    /**
     * Returns the number of failed attempts for the given username.
     *
     * @param username the account identifier
     * @return failed attempt count, or 0 if no record exists
     */
    public int getFailedAttempts(String username) {
        LockoutState state = lockoutStates.get(username);
        return state != null ? state.failedAttempts() : 0;
    }

    private record LockoutState(int failedAttempts, Instant lockedUntil) {}
}
