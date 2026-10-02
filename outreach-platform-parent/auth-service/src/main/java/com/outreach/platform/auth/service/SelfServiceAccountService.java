package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.AccountToken;
import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.model.AccountStatus;
import com.outreach.platform.auth.model.AccountTokenPurpose;
import com.outreach.platform.auth.repo.UserAccountRepository;
import com.outreach.platform.common.pii.PiiBlindIndex;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * The unauthenticated account flows behind the auth server's own pages: activating an invitation
 * and resetting a forgotten password. Responses never reveal whether an account exists.
 */
@Named
public class SelfServiceAccountService {

    private static final Logger log = LoggerFactory.getLogger(SelfServiceAccountService.class);

    private final UserAccountRepository accounts;
    private final AccountTokenService tokens;
    private final PasswordService passwords;
    private final AccountAdminService admin;
    private final IdentityEventPublisher events;

    @Inject
    public SelfServiceAccountService(UserAccountRepository accounts, AccountTokenService tokens,
                                     PasswordService passwords, AccountAdminService admin,
                                     IdentityEventPublisher events) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.passwords = passwords;
        this.admin = admin;
        this.events = events;
    }

    /** The account a link belongs to, if the link is still valid. */
    @Transactional(readOnly = true)
    public Optional<UserAccount> accountForLink(String rawToken, AccountTokenPurpose purpose) {
        return tokens.findUsable(rawToken, purpose)
                .flatMap(token -> accounts.findById(token.getUserId()))
                .filter(account -> expectedStatus(purpose) == account.getStatus());
    }

    /** Policy problems with a candidate password for the account behind a link (shown before submitting). */
    @Transactional(readOnly = true)
    public List<String> checkPassword(UserAccount account, String candidate) {
        return passwords.check(account, candidate);
    }

    /**
     * Sets the password from an activation or reset link and spends the link.
     *
     * @throws InvalidLinkException when the link is unknown, used or expired
     * @throws PasswordService.PasswordPolicyViolationException when the password is not acceptable
     *         (the link stays valid so the user can try again)
     */
    @Transactional
    public UserAccount completeLink(String rawToken, AccountTokenPurpose purpose, String newPassword) {
        UserAccount account = accountForLink(rawToken, purpose).orElseThrow(InvalidLinkException::new);
        boolean activating = purpose == AccountTokenPurpose.ACTIVATION;
        passwords.setPassword(account, newPassword, !activating);
        AccountToken token = tokens.consume(rawToken, purpose).orElseThrow(InvalidLinkException::new);
        if (activating) {
            account.setStatus(AccountStatus.ACTIVE);
            events.userChanged(account);
        } else {
            // Any other reset links still in someone's inbox stop working.
            tokens.revokeOutstanding(account.getId(), purpose);
        }
        log.info("Account link completed: purpose={}, userId={}, tokenId={}", purpose, account.getId(), token.getId());
        return account;
    }

    /**
     * "Forgot password": emails a reset link if the username or email matches an active account,
     * and does nothing otherwise. The caller shows the same confirmation either way.
     */
    @Transactional
    public void requestPasswordReset(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return;
        }
        String identifier = usernameOrEmail.trim();
        Optional<UserAccount> account = identifier.contains("@")
                ? accounts.findByEmailHash(PiiBlindIndex.of(identifier))
                : accounts.findByUsername(identifier.toLowerCase(java.util.Locale.ROOT));
        account.filter(a -> a.getStatus() == AccountStatus.ACTIVE && a.getEmail() != null)
                .ifPresent(admin::issuePasswordReset);
    }

    private static AccountStatus expectedStatus(AccountTokenPurpose purpose) {
        return purpose == AccountTokenPurpose.ACTIVATION ? AccountStatus.INVITED : AccountStatus.ACTIVE;
    }

    public static class InvalidLinkException extends RuntimeException {
        public InvalidLinkException() {
            super("This link is invalid or has expired");
        }
    }
}
