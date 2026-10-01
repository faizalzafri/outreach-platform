package com.outreach.platform.event.service;

import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import com.outreach.platform.common.pii.AesEncryptionConverter;
import com.outreach.platform.common.tenant.TenantContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the {@code users} directory in step with auth-service, which owns accounts. Each
 * {@code identity.user-changed} event (one per tenant membership) upserts the row under the
 * account's own id, so POC assignments and team memberships keep pointing at the same user.
 *
 * <p>Plain SQL rather than JPA: the row must take auth-service's id, which the entity's generated
 * id cannot accept on insert.
 */
// ponytail: one directory row per user. A user in two tenants ends up under the tenant of the
// latest event; split the table per (user, tenant) if multi-tenant users become common.
@Service
public class UserDirectorySync {

    private static final Logger log = LoggerFactory.getLogger(UserDirectorySync.class);
    private static final Set<String> DIRECTORY_ROLES = Set.of("ADMIN", "PMO", "POC");
    private static final String SYNC_USER = "auth-service";

    private final JdbcTemplate jdbc;
    private final AesEncryptionConverter pii = new AesEncryptionConverter();

    @Inject
    public UserDirectorySync(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @RabbitListener(queues = RabbitMqConstants.QUEUE_EVENT_IDENTITY)
    public void onUserChanged(DomainEventMessage message) {
        apply(TenantContext.getCurrentTenantId(), message.payload());
    }

    @Transactional
    public void apply(UUID tenantId, Map<String, Object> payload) {
        UUID userId = UUID.fromString(String.valueOf(payload.get("userId")));
        String username = String.valueOf(payload.get("username"));
        String role = String.valueOf(payload.get("role"));
        if (!DIRECTORY_ROLES.contains(role)) {
            return; // platform admins are not tenant members, so they are not in a tenant's directory
        }
        Object email = payload.get("email");
        boolean enabled = "ACTIVE".equals(payload.get("status"));

        // A row created before auth-service owned accounts may hold this username under another id.
        // Free the name but keep the row, so anything that references it stays valid.
        jdbc.update("UPDATE users SET username = username || '~' || left(id::text, 8), enabled = FALSE"
                + " WHERE username = ? AND id <> ?", username, userId);

        jdbc.update("""
                INSERT INTO users (id, username, display_name, email_encrypted, role, enabled, tenant_id,
                                   created_at, updated_at, created_by, updated_by, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?, 0)
                ON CONFLICT (id) DO UPDATE SET
                    username = EXCLUDED.username,
                    display_name = EXCLUDED.display_name,
                    email_encrypted = EXCLUDED.email_encrypted,
                    role = EXCLUDED.role,
                    enabled = EXCLUDED.enabled,
                    tenant_id = EXCLUDED.tenant_id,
                    updated_at = NOW(),
                    updated_by = EXCLUDED.updated_by,
                    version = users.version + 1
                """,
                userId, username, payload.get("displayName"),
                pii.convertToDatabaseColumn(email == null ? "" : email.toString()),
                role, enabled, tenantId, SYNC_USER, SYNC_USER);
        log.info("Directory updated from identity event: userId={}, role={}, enabled={}", userId, role, enabled);
    }
}
