package com.outreach.platform.event.integration;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.entity.BeneficiaryEntity;
import com.outreach.platform.event.entity.Team;
import com.outreach.platform.event.entity.UserEntity;
import com.outreach.platform.event.model.UserRole;
import com.outreach.platform.event.repo.BeneficiaryRepository;
import com.outreach.platform.event.repo.TeamRepository;
import com.outreach.platform.event.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-tenant leakage regression tests for the remaining repositories {@code findByIdAndTenantId}
 * was added to as part of the findById fix (see CLAUDE.md) — {@link UserRepository}, {@link BeneficiaryRepository},
 * {@link TeamRepository}. {@code EventEntity} already has
 * its own dedicated test ({@link EventTenantIsolationIT}); this class covers the rest of Task
 * 0.5.6 for event-service so every fixed repository has proof the fix actually blocks a
 * cross-tenant read, not just that the method compiles.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
class TenantScopedRepositoriesIsolationIT {

    static {
        // UserEntity.email is a @PiiField, encrypted via AesEncryptionConverter, which requires
        // this key (env var or, as here, the system-property fallback the converter documents as
        // "for testing") to be set before any UserEntity is persisted — otherwise the encrypt step
        // throws PiiEncryptionException and the whole save() transaction rolls back.
        System.setProperty("pii.encryption.key", "0".repeat(63) + "1");
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("event_test")
            .withUsername("test")
            .withPassword("test");

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private TeamRepository teamRepository;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void userRepository_crossTenantRead_returnsEmpty() {
        UUID tenantA = seedTenant("User Tenant A", "user-tenant-a");
        UUID tenantB = seedTenant("User Tenant B", "user-tenant-b");

        TenantContext.setCurrentTenantId(tenantA);
        UserEntity saved = userRepository.save(newUser("user-a"));

        TenantContext.setCurrentTenantId(tenantB);
        Optional<UserEntity> readAsTenantB = userRepository.findByIdAndTenantId(saved.getId(), tenantB);

        assertThat(readAsTenantB)
                .as("tenant B must not be able to read a user created under tenant A")
                .isEmpty();
    }

    @Test
    void userRepository_sameTenantRead_findsTheUser() {
        UUID tenantA = seedTenant("User Tenant C", "user-tenant-c");

        TenantContext.setCurrentTenantId(tenantA);
        UserEntity saved = userRepository.save(newUser("user-c"));

        Optional<UserEntity> readBack = userRepository.findByIdAndTenantId(saved.getId(), tenantA);

        assertThat(readBack).as("a tenant must still be able to read its own data").isPresent();
        assertThat(readBack.get().getTenantId()).isEqualTo(tenantA);
    }

    @Test
    void beneficiaryRepository_crossTenantRead_returnsEmpty() {
        UUID tenantA = seedTenant("Beneficiary Tenant A", "beneficiary-tenant-a");
        UUID tenantB = seedTenant("Beneficiary Tenant B", "beneficiary-tenant-b");

        TenantContext.setCurrentTenantId(tenantA);
        BeneficiaryEntity saved = beneficiaryRepository.save(newBeneficiary("Beneficiary A"));

        TenantContext.setCurrentTenantId(tenantB);
        Optional<BeneficiaryEntity> readAsTenantB = beneficiaryRepository.findByIdAndTenantId(saved.getId(), tenantB);

        assertThat(readAsTenantB)
                .as("tenant B must not be able to read a beneficiary created under tenant A")
                .isEmpty();
    }

    @Test
    void beneficiaryRepository_sameTenantRead_findsTheBeneficiary() {
        UUID tenantA = seedTenant("Beneficiary Tenant C", "beneficiary-tenant-c");

        TenantContext.setCurrentTenantId(tenantA);
        BeneficiaryEntity saved = beneficiaryRepository.save(newBeneficiary("Beneficiary C"));

        Optional<BeneficiaryEntity> readBack = beneficiaryRepository.findByIdAndTenantId(saved.getId(), tenantA);

        assertThat(readBack).as("a tenant must still be able to read its own data").isPresent();
        assertThat(readBack.get().getTenantId()).isEqualTo(tenantA);
    }

    @Test
    void teamRepository_crossTenantRead_returnsEmpty() {
        UUID tenantA = seedTenant("Team Tenant A", "team-tenant-a");
        UUID tenantB = seedTenant("Team Tenant B", "team-tenant-b");

        TenantContext.setCurrentTenantId(tenantA);
        Team saved = teamRepository.save(newTeam("Team A"));

        TenantContext.setCurrentTenantId(tenantB);
        Optional<Team> readAsTenantB = teamRepository.findByIdAndTenantId(saved.getId(), tenantB);

        assertThat(readAsTenantB)
                .as("tenant B must not be able to read a team created under tenant A")
                .isEmpty();
    }

    @Test
    void teamRepository_sameTenantRead_findsTheTeam() {
        UUID tenantA = seedTenant("Team Tenant C", "team-tenant-c");

        TenantContext.setCurrentTenantId(tenantA);
        Team saved = teamRepository.save(newTeam("Team C"));

        Optional<Team> readBack = teamRepository.findByIdAndTenantId(saved.getId(), tenantA);

        assertThat(readBack).as("a tenant must still be able to read its own data").isPresent();
        assertThat(readBack.get().getTenantId()).isEqualTo(tenantA);
    }

    // Organizations belong to auth-service; event tables only need the row their tenant_id
    // foreign key points at.
    private UUID seedTenant(String name, String slug) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO tenants (id, name, slug, status, created_date, created_by, version) "
                + "VALUES (?, ?, ?, 'ACTIVE', now(), 'test', 0)", id, name, slug);
        return id;
    }

    private UserEntity newUser(String username) {
        UserEntity user = new UserEntity();
        user.setUsername(username + "-" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail("encrypted-placeholder");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        return user;
    }

    private BeneficiaryEntity newBeneficiary(String name) {
        BeneficiaryEntity beneficiary = new BeneficiaryEntity();
        beneficiary.setName(name);
        beneficiary.setActive(true);
        return beneficiary;
    }

    private Team newTeam(String name) {
        Team team = new Team();
        team.setName(name);
        return team;
    }
}
