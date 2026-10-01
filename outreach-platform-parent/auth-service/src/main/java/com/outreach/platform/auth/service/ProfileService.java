package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.repo.UserAccountRepository;
import com.outreach.platform.auth.model.OtpPurpose;
import com.outreach.platform.auth.service.otp.OtpService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A signed-in user's own account: view and edit their details, change their password. */
@Named
public class ProfileService {

    private final UserAccountRepository accounts;
    private final PasswordService passwords;
    private final IdentityEventPublisher events;
    private final SecurityPolicyService policies;
    private final OtpService otp;

    @Inject
    public ProfileService(UserAccountRepository accounts, PasswordService passwords,
                          IdentityEventPublisher events, SecurityPolicyService policies, OtpService otp) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.events = events;
        this.policies = policies;
        this.otp = otp;
    }

    public record Profile(UUID id, String username, String displayName, String email, String phone,
                          boolean platformAdmin, Instant lastLoginAt, Instant passwordChangedAt,
                          int passwordMinLength) {
    }

    @Transactional(readOnly = true)
    public Profile get(String username) {
        return profile(account(username));
    }

    @Transactional
    public Profile update(String username, String displayName, String phone) {
        UserAccount account = account(username);
        if (displayName != null && !displayName.isBlank()) {
            account.setDisplayName(displayName.trim());
        }
        account.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        events.userChanged(account);
        return profile(account);
    }

    /**
     * Changes the password after checking the current one.
     *
     * @throws WrongPasswordException when the current password is wrong
     * @throws PasswordService.PasswordPolicyViolationException when the new password is not acceptable
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword, String otpCode) {
        UserAccount account = account(username);
        if (!passwords.matches(account, currentPassword)) {
            throw new WrongPasswordException();
        }
        // Settle the new password first, so a code is only sent once the change can actually go through.
        List<String> violations = passwords.check(account, newPassword);
        if (!violations.isEmpty()) {
            throw new PasswordService.PasswordPolicyViolationException(violations);
        }
        if (otp.isRequired(account, OtpPurpose.PASSWORD_CHANGE)) {
            if (otpCode == null || otpCode.isBlank()) {
                sendCode(account);
                throw new OtpRequiredException("Enter the verification code we emailed you");
            }
            OtpService.Verification check = otp.verify(account, OtpPurpose.PASSWORD_CHANGE, otpCode);
            if (check.outcome() == OtpService.Outcome.WRONG) {
                throw new OtpRejectedException("That code isn't right (" + check.attemptsLeft() + " attempts left)");
            }
            if (check.outcome() == OtpService.Outcome.EXPIRED) {
                sendCode(account);
                throw new OtpRejectedException("That code expired; we've emailed you a new one");
            }
        }
        passwords.setPassword(account, newPassword, true);
    }

    private void sendCode(UserAccount account) {
        try {
            otp.issueIfNoneOpen(account, OtpPurpose.PASSWORD_CHANGE);
        } catch (OtpService.OtpCooldownException justSent) {
            // the previous code is still on its way
        }
    }

    /** The password change needs a one-time code; one has been sent. */
    public static class OtpRequiredException extends RuntimeException {
        public OtpRequiredException(String message) {
            super(message);
        }
    }

    /** The one-time code given was wrong or expired. */
    public static class OtpRejectedException extends RuntimeException {
        public OtpRejectedException(String message) {
            super(message);
        }
    }

    @Transactional(readOnly = true)
    public List<String> checkPassword(String username, String candidate) {
        return passwords.check(account(username), candidate);
    }

    UserAccount account(String username) {
        return accounts.findByUsername(username)
                .orElseThrow(() -> new AccountAdminService.AccountNotFoundException(null));
    }

    private Profile profile(UserAccount account) {
        return new Profile(account.getId(), account.getUsername(), account.getDisplayName(), account.getEmail(),
                account.getPhone(), account.isPlatformAdmin(), account.getLastLoginAt(),
                account.getPasswordChangedAt(), policies.passwordMinLength(account));
    }

    public static class WrongPasswordException extends RuntimeException {
        public WrongPasswordException() {
            super("Your current password is incorrect");
        }
    }
}
