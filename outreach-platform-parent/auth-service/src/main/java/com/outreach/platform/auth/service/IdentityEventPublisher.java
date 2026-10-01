package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.OutboxMessage;
import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.repo.OutboxMessageRepository;
import com.outreach.platform.auth.repo.TenantMembershipRepository;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records identity events in the outbox, inside the caller's transaction, so an email or directory
 * update is sent if and only if the account change commits. {@link OutboxPublisher} delivers them.
 *
 * <p>Payloads carry what the consumer needs to act (recipient, display name, link) and never a
 * password. The OTP event necessarily carries the code: it travels only on the internal broker and
 * is never logged.
 */
@Named
public class IdentityEventPublisher {

    private final OutboxMessageRepository outbox;
    private final TenantMembershipRepository memberships;

    @Inject
    public IdentityEventPublisher(OutboxMessageRepository outbox, TenantMembershipRepository memberships) {
        this.outbox = outbox;
        this.memberships = memberships;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void userInvited(UserAccount account, String activationLink, Instant expiresAt, String invitedBy) {
        Map<String, Object> payload = contact(account);
        payload.put("activationLink", activationLink);
        payload.put("expiresAt", expiresAt.toString());
        payload.put("invitedBy", invitedBy);
        write(RabbitMqConstants.ROUTING_KEY_IDENTITY_USER_INVITED, tenantOf(account), payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void passwordResetRequested(UserAccount account, String resetLink, Instant expiresAt) {
        Map<String, Object> payload = contact(account);
        payload.put("resetLink", resetLink);
        payload.put("expiresAt", expiresAt.toString());
        write(RabbitMqConstants.ROUTING_KEY_IDENTITY_PASSWORD_RESET_REQUESTED, tenantOf(account), payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void passwordChanged(UserAccount account) {
        Map<String, Object> payload = contact(account);
        payload.put("changedAt", Instant.now().toString());
        write(RabbitMqConstants.ROUTING_KEY_IDENTITY_PASSWORD_CHANGED, tenantOf(account), payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void otpIssued(UserAccount account, String channel, String purpose, String code, Instant expiresAt) {
        Map<String, Object> payload = contact(account);
        payload.put("channel", channel);
        payload.put("purpose", purpose);
        payload.put("code", code);
        payload.put("expiresAt", expiresAt.toString());
        write(RabbitMqConstants.ROUTING_KEY_IDENTITY_OTP_ISSUED, tenantOf(account), payload);
    }

    /**
     * Directory update for other services: one event per tenant the user belongs to, so each
     * tenant-scoped consumer sees the user under its own tenant.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void userChanged(UserAccount account) {
        var userMemberships = memberships.findByUserId(account.getId());
        for (var membership : userMemberships) {
            Map<String, Object> payload = contact(account);
            payload.put("username", account.getUsername());
            payload.put("status", account.getStatus().name());
            payload.put("role", membership.getRole().name());
            write(RabbitMqConstants.ROUTING_KEY_IDENTITY_USER_CHANGED, membership.getTenantId(), payload);
        }
    }

    private static Map<String, Object> contact(UserAccount account) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", account.getId().toString());
        payload.put("email", account.getEmail());
        payload.put("displayName", account.getDisplayName());
        return payload;
    }

    /** Messages go out under the user's first tenant; platform admins have none. */
    private UUID tenantOf(UserAccount account) {
        List<UUID> tenants = memberships.findByUserId(account.getId()).stream()
                .map(m -> m.getTenantId())
                .toList();
        return tenants.isEmpty() ? RabbitMqConstants.PLATFORM_TENANT_ID : tenants.getFirst();
    }

    private void write(String routingKey, UUID tenantId, Map<String, Object> payload) {
        OutboxMessage message = new OutboxMessage();
        message.setRoutingKey(routingKey);
        message.setTenantId(tenantId);
        message.setPayload(payload);
        outbox.save(message);
    }
}
