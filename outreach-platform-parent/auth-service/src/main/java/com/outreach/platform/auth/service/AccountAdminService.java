package com.outreach.platform.auth.service;

import com.outreach.platform.auth.config.AuthServiceProperties;
import com.outreach.platform.auth.entity.TenantMembership;
import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.model.AccountStatus;
import com.outreach.platform.auth.model.AccountTokenPurpose;
import com.outreach.platform.auth.model.TenantRole;
import com.outreach.platform.auth.repo.TenantMembershipRepository;
import com.outreach.platform.auth.repo.UserAccountRepository;
import com.outreach.platform.common.pii.PiiBlindIndex;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * User administration within a tenant: invite, edit, change role, enable/disable, resend or revoke
 * an invitation, and send a password reset. Admins never set or see passwords: new users choose
 * their own from the activation link.
 *
 * <p>Every operation is scoped to one tenant. Callers resolve that tenant from the caller's token
 * (or, for platform admins, an explicit choice) and this service refuses to touch accounts that
 * are not members of it.
 */
@Named
public class AccountAdminService {

    private final UserAccountRepository accounts;
    private final TenantMembershipRepository memberships;
    private final AccountTokenService tokens;
    private final IdentityEventPublisher events;
    private final AuthServiceProperties properties;

    @Inject
    public AccountAdminService(UserAccountRepository accounts, TenantMembershipRepository memberships,
                               AccountTokenService tokens, IdentityEventPublisher events,
                               AuthServiceProperties properties) {
        this.accounts = accounts;
        this.memberships = memberships;
        this.tokens = tokens;
        this.events = events;
        this.properties = properties;
    }

    public record UserSummary(UUID id, String username, String displayName, String email, String phone,
                              TenantRole role, AccountStatus status, boolean locked,
                              Instant lastLoginAt, Instant createdAt) {
    }

    public record InviteCommand(String username, String email, String displayName, TenantRole role) {
    }

    @Transactional(readOnly = true)
    public List<UserSummary> list(UUID tenantId) {
        List<TenantMembership> tenantMembers = memberships.findByTenantId(tenantId);
        Map<UUID, UserAccount> byId = accounts.findByIdIn(
                        tenantMembers.stream().map(TenantMembership::getUserId).toList()).stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));
        Instant now = Instant.now();
        return tenantMembers.stream()
                .filter(m -> byId.containsKey(m.getUserId()))
                .map(m -> summary(byId.get(m.getUserId()), m.getRole(), now))
                .sorted(Comparator.comparing(UserSummary::username))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserSummary get(UUID tenantId, UUID userId) {
        TenantMembership membership = membershipOrThrow(tenantId, userId);
        return summary(accountOrThrow(userId), membership.getRole(), Instant.now());
    }

    @Transactional
    public UserSummary invite(UUID tenantId, InviteCommand command, String invitedBy) {
        String username = command.username().trim().toLowerCase(java.util.Locale.ROOT);
        if (accounts.existsByUsername(username)) {
            throw new AccountConflictException("Username '" + username + "' is already taken");
        }
        if (accounts.existsByEmailHash(PiiBlindIndex.of(command.email()))) {
            throw new AccountConflictException("An account with this email address already exists");
        }
        if (command.role() == TenantRole.PLATFORM_ADMIN) {
            throw new IllegalArgumentException("Platform admins are not tenant members");
        }

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setEmail(command.email());
        account.setDisplayName(command.displayName().trim());
        account.setStatus(AccountStatus.INVITED);
        accounts.save(account);

        TenantMembership membership = new TenantMembership();
        membership.setTenantId(tenantId);
        membership.setUserId(account.getId());
        membership.setRole(command.role());
        memberships.save(membership);

        sendInvitation(account, invitedBy);
        events.userChanged(account);
        return summary(account, command.role(), Instant.now());
    }

    /** Creates an invited platform admin (no tenant). Used by the startup bootstrap. */
    @Transactional
    public UserAccount invitePlatformAdmin(String username, String email, String invitedBy) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setEmail(email);
        account.setDisplayName("Platform Admin");
        account.setStatus(AccountStatus.INVITED);
        account.setPlatformAdmin(true);
        accounts.save(account);
        sendInvitation(account, invitedBy);
        return account;
    }

    @Transactional
    public UserSummary updateProfile(UUID tenantId, UUID userId, String displayName, String phone) {
        TenantMembership membership = membershipOrThrow(tenantId, userId);
        UserAccount account = accountOrThrow(userId);
        if (displayName != null && !displayName.isBlank()) {
            account.setDisplayName(displayName.trim());
        }
        account.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        events.userChanged(account);
        return summary(account, membership.getRole(), Instant.now());
    }

    @Transactional
    public UserSummary changeRole(UUID tenantId, UUID userId, TenantRole role, UUID actingUserId) {
        if (userId.equals(actingUserId)) {
            throw new IllegalArgumentException("You cannot change your own role");
        }
        if (role == TenantRole.PLATFORM_ADMIN) {
            throw new IllegalArgumentException("Platform admins are not tenant members");
        }
        TenantMembership membership = membershipOrThrow(tenantId, userId);
        membership.setRole(role);
        UserAccount account = accountOrThrow(userId);
        events.userChanged(account);
        return summary(account, role, Instant.now());
    }

    @Transactional
    public UserSummary setEnabled(UUID tenantId, UUID userId, boolean enabled, UUID actingUserId) {
        if (!enabled && userId.equals(actingUserId)) {
            throw new IllegalArgumentException("You cannot disable your own account");
        }
        TenantMembership membership = membershipOrThrow(tenantId, userId);
        UserAccount account = accountOrThrow(userId);
        if (account.getStatus() == AccountStatus.INVITED) {
            throw new IllegalStateException("This user has not activated their account yet");
        }
        account.setStatus(enabled ? AccountStatus.ACTIVE : AccountStatus.DISABLED);
        if (enabled) {
            account.setLockedUntil(null);
            account.setFailedLoginAttempts(0);
        }
        events.userChanged(account);
        return summary(account, membership.getRole(), Instant.now());
    }

    @Transactional
    public void resendInvitation(UUID tenantId, UUID userId, String invitedBy) {
        membershipOrThrow(tenantId, userId);
        UserAccount account = accountOrThrow(userId);
        if (account.getStatus() != AccountStatus.INVITED) {
            throw new IllegalStateException("This user has already activated their account");
        }
        sendInvitation(account, invitedBy);
    }

    /** Withdraws a pending invitation: the link stops working and the user is removed from the tenant. */
    @Transactional
    public void revokeInvitation(UUID tenantId, UUID userId) {
        TenantMembership membership = membershipOrThrow(tenantId, userId);
        UserAccount account = accountOrThrow(userId);
        if (account.getStatus() != AccountStatus.INVITED) {
            throw new IllegalStateException("Only pending invitations can be revoked; disable active users instead");
        }
        tokens.revokeOutstanding(userId, AccountTokenPurpose.ACTIVATION);
        memberships.delete(membership);
        if (memberships.findByUserId(userId).isEmpty()) {
            accounts.delete(account);
        }
    }

    @Transactional
    public void sendPasswordReset(UUID tenantId, UUID userId) {
        membershipOrThrow(tenantId, userId);
        UserAccount account = accountOrThrow(userId);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Password resets can only be sent to active users");
        }
        issuePasswordReset(account);
    }

    /** Emails a reset link; shared with the self-service "forgot password" flow. */
    @Transactional
    public void issuePasswordReset(UserAccount account) {
        var token = tokens.issue(account.getId(), AccountTokenPurpose.PASSWORD_RESET,
                properties.accounts().passwordResetTtl());
        events.passwordResetRequested(account,
                properties.publicUrl() + "/reset-password?token=" + token.rawToken(), token.expiresAt());
    }

    private void sendInvitation(UserAccount account, String invitedBy) {
        var token = tokens.issue(account.getId(), AccountTokenPurpose.ACTIVATION,
                properties.accounts().activationTtl());
        events.userInvited(account, properties.publicUrl() + "/activate?token=" + token.rawToken(),
                token.expiresAt(), invitedBy);
    }

    private TenantMembership membershipOrThrow(UUID tenantId, UUID userId) {
        return memberships.findByUserId(userId).stream()
                .filter(m -> m.getTenantId().equals(tenantId))
                .findFirst()
                .orElseThrow(() -> new AccountNotFoundException(userId));
    }

    private UserAccount accountOrThrow(UUID userId) {
        return accounts.findById(userId).orElseThrow(() -> new AccountNotFoundException(userId));
    }

    private static UserSummary summary(UserAccount account, TenantRole role, Instant now) {
        return new UserSummary(account.getId(), account.getUsername(), account.getDisplayName(),
                account.getEmail(), account.getPhone(), role, account.getStatus(), account.isLocked(now),
                account.getLastLoginAt(), account.getCreatedDate());
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(UUID userId) {
            super("User " + userId + " not found");
        }
    }

    public static class AccountConflictException extends RuntimeException {
        public AccountConflictException(String message) {
            super(message);
        }
    }
}
