package com.outreach.platform.event.integration;

import com.outreach.platform.common.pii.AesEncryptionConverter;
import com.outreach.platform.event.service.UserDirectorySync;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The user directory follows auth-service's identity events: rows are created and updated under
 * the account's own id, and an old row holding the same username is retired, not overwritten.
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
@ActiveProfiles("test")
@Testcontainers
class UserDirectorySyncIT {

    static {
        System.setProperty("pii.encryption.key", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    private static final UUID DEFAULT_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("event_test").withUsername("test").withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private UserDirectorySync sync;

    @Autowired
    private JdbcTemplate jdbc;

    private final AesEncryptionConverter pii = new AesEncryptionConverter();

    @Test
    void invitedThenActivatedUser_isCreatedThenUpdatedUnderTheAccountId() {
        UUID id = UUID.randomUUID();
        String username = "kavya_" + id.toString().substring(0, 6);

        sync.apply(DEFAULT_TENANT, event(id, username, "Kavya Rao", "kavya@example.com", "POC", "INVITED"));
        Map<String, Object> invited = row(id);
        assertThat(invited.get("display_name")).isEqualTo("Kavya Rao");
        assertThat(invited.get("enabled")).isEqualTo(false);
        assertThat(pii.convertToEntityAttribute((String) invited.get("email_encrypted"))).isEqualTo("kavya@example.com");

        sync.apply(DEFAULT_TENANT, event(id, username, "Kavya R.", "kavya@example.com", "PMO", "ACTIVE"));
        Map<String, Object> active = row(id);
        assertThat(active.get("display_name")).isEqualTo("Kavya R.");
        assertThat(active.get("role")).isEqualTo("PMO");
        assertThat(active.get("enabled")).isEqualTo(true);
        assertThat((Long) active.get("version")).isEqualTo(1L);
    }

    @Test
    void olderRowWithTheSameUsername_isRetiredNotOverwritten() {
        UUID legacyId = UUID.randomUUID();
        String username = "legacy_" + legacyId.toString().substring(0, 6);
        jdbc.update("""
                INSERT INTO users (id, username, email_encrypted, role, enabled, tenant_id, created_by)
                VALUES (?, ?, ?, 'POC', TRUE, ?, 'test')""",
                legacyId, username, pii.convertToDatabaseColumn("old@example.com"), DEFAULT_TENANT);

        UUID accountId = UUID.randomUUID();
        sync.apply(DEFAULT_TENANT, event(accountId, username, "New Person", "new@example.com", "POC", "ACTIVE"));

        assertThat(row(accountId).get("username")).isEqualTo(username);
        Map<String, Object> legacy = row(legacyId);
        assertThat((String) legacy.get("username")).startsWith(username + "~");
        assertThat(legacy.get("enabled")).isEqualTo(false);
    }

    @Test
    void platformAdmins_areNotPartOfATenantDirectory() {
        UUID id = UUID.randomUUID();
        sync.apply(DEFAULT_TENANT, event(id, "pa_" + id.toString().substring(0, 6), "Platform", "pa@example.com",
                "PLATFORM_ADMIN", "ACTIVE"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id)).isZero();
    }

    private Map<String, Object> row(UUID id) {
        return jdbc.queryForMap("SELECT * FROM users WHERE id = ?", id);
    }

    private static Map<String, Object> event(UUID id, String username, String displayName, String email,
                                             String role, String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", id.toString());
        payload.put("username", username);
        payload.put("displayName", displayName);
        payload.put("email", email);
        payload.put("role", role);
        payload.put("status", status);
        return payload;
    }
}
