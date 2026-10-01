package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.PasswordHistoryEntry;
import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.repo.PasswordHistoryRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Sets an account's password under the full policy: rules, guessability, and no reuse of recent
 * passwords. Every successful change is recorded in history, clears any lockout, and sends the
 * user a "your password was changed" notice.
 */
@Named
public class PasswordService {

    private final PasswordEncoder encoder;
    private final PasswordPolicyValidator validator;
    private final PasswordHistoryRepository history;
    private final SecurityPolicyService policies;
    private final IdentityEventPublisher events;

    @Inject
    public PasswordService(PasswordEncoder encoder, PasswordPolicyValidator validator,
                           PasswordHistoryRepository history, SecurityPolicyService policies,
                           IdentityEventPublisher events) {
        this.encoder = encoder;
        this.validator = validator;
        this.history = history;
        this.policies = policies;
        this.events = events;
    }

    /** Problems with the candidate for this account; empty when it is acceptable. */
    @Transactional(readOnly = true)
    public List<String> check(UserAccount account, String candidate) {
        List<String> violations = new ArrayList<>(validator.validate(candidate, account.getUsername(),
                account.getEmail(), policies.passwordMinLength(account)));
        if (candidate != null && reusesRecentPassword(account, candidate)) {
            violations.add("Password was used recently; choose one you have not used before");
        }
        return violations;
    }

    /**
     * Sets the password if it passes {@link #check}; otherwise throws with the violations.
     * {@code notify} is false only when the user is setting their first password (activation).
     */
    @Transactional
    public void setPassword(UserAccount account, String candidate, boolean notify) {
        List<String> violations = check(account, candidate);
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }
        String encoded = encoder.encode(candidate);
        account.setPassword(encoded);
        account.setPasswordChangedAt(Instant.now());
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        history.save(new PasswordHistoryEntry(account.getId(), encoded));
        trimHistory(account);
        if (notify) {
            events.passwordChanged(account);
        }
    }

    public boolean matches(UserAccount account, String raw) {
        return account.getPassword() != null && raw != null && encoder.matches(raw, account.getPassword());
    }

    private boolean reusesRecentPassword(UserAccount account, String candidate) {
        if (matches(account, candidate)) {
            return true;
        }
        int keep = policies.passwordHistoryCount(account);
        return history.findByUserIdOrderByCreatedDateDesc(account.getId()).stream()
                .limit(keep)
                .anyMatch(entry -> encoder.matches(candidate, entry.getPasswordHash()));
    }

    private void trimHistory(UserAccount account) {
        int keep = Math.max(policies.passwordHistoryCount(account), 1);
        List<PasswordHistoryEntry> entries = history.findByUserIdOrderByCreatedDateDesc(account.getId());
        if (entries.size() > keep) {
            history.deleteAll(entries.subList(keep, entries.size()));
        }
    }

    public static class PasswordPolicyViolationException extends RuntimeException {
        private final List<String> violations;

        public PasswordPolicyViolationException(List<String> violations) {
            super(String.join("; ", violations));
            this.violations = List.copyOf(violations);
        }

        public List<String> violations() {
            return violations;
        }
    }
}
