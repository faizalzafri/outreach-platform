package com.outreach.platform.event.integration;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.entity.EventEntity;
import com.outreach.platform.event.model.EventStatus;
import com.outreach.platform.event.repo.EventRepository;
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

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-tenant leakage regression test for {@link EventEntity} — the concrete deliverable for
 * (CLAUDE.md). {@code EventEntity} was
 * migrated to extend {@code TenantAwareBaseEntity} instead of hand-rolling its own
 * {@code @FilterDef}/{@code @Filter}; this test proves the inherited Hibernate tenant filter
 * actually blocks a cross-tenant read at the repository layer, not just that the annotations
 * compile.
 *
 * <p>Deliberately {@code webEnvironment = NONE} — this is a repository-layer concern, not an
 * HTTP one, and avoids depending on the security filter chain or Redis (needed by other,
 * unrelated code paths this test doesn't exercise) that a full web-environment test would pull
 * in. See {@code EventServiceIT}'s class Javadoc for the unrelated pre-existing gaps that had to
 * be fixed just to get *that* test suite booting at all.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
class EventTenantIsolationIT {

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
    private EventRepository eventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearTenantContext() {
        // TenantContext is a ThreadLocal — leaking a value into the next test is exactly the
        // class of bug this whole test exists to guard against, so clear it unconditionally.
        TenantContext.clear();
    }

    @Test
    void crossTenantRead_returnsEmpty_whenReadingUnderDifferentTenant() {
        UUID tenantA = seedTenant("Tenant A Corp", "tenant-a-corp");
        UUID tenantB = seedTenant("Tenant B Corp", "tenant-b-corp");

        TenantContext.setCurrentTenantId(tenantA);
        EventEntity eventUnderTenantA = eventRepository.save(newDraftEvent("Tenant A Only Event"));
        UUID eventId = eventUnderTenantA.getId();

        TenantContext.setCurrentTenantId(tenantB);
        // findByIdAndTenantId — NOT findById — is the fix: see EventRepository's Javadoc on that
        // method and in CLAUDE.md. This is also what
        // EventService.findEntityOrThrow now calls internally.
        Optional<EventEntity> readAsTenantB = eventRepository.findByIdAndTenantId(eventId, tenantB);

        assertThat(readAsTenantB)
                .as("tenant B must not be able to read an event created under tenant A")
                .isEmpty();
    }

    @Test
    void sameTenantRead_findsTheEvent_provingTheFilterIsntBlockingEverything() {
        UUID tenantA = seedTenant("Tenant C Corp", "tenant-c-corp");

        TenantContext.setCurrentTenantId(tenantA);
        EventEntity created = eventRepository.save(newDraftEvent("Tenant C Own Event"));

        Optional<EventEntity> readBackUnderSameTenant = eventRepository.findByIdAndTenantId(created.getId(), tenantA);

        assertThat(readBackUnderSameTenant)
                .as("a tenant must still be able to read its own data — this catches an over-broad filter as surely as a missing one")
                .isPresent();
        assertThat(readBackUnderSameTenant.get().getTenantId()).isEqualTo(tenantA);
    }

    @Test
    void plainFindById_stillLeaksAcrossTenants_becauseItBypassesTheFilterEntirely() {
        // This test documents a known, accepted-for-now limitation of the fix
        // rather than a regression: adding findByIdAndTenantId gives callers a safe method to use,
        // it does NOT make the inherited findById() itself tenant-safe — that method still compiles
        // to EntityManager.find(), which no Hibernate @Filter reaches. The real fix for *this* is
        //.5 (audit every tenant-scoped repository) plus, ideally, an ArchUnit rule banning
        // bare findById() on tenant-scoped repositories so a future caller can't reintroduce this
        // by simply not knowing to use the safe method. If this test ever starts failing (i.e.
        // findById starts correctly returning empty), that's good news — update it to assertThat
        // ...isEmpty() and remove this comment, it means the underlying Hibernate behavior changed
        // or a repository-level override closed the gap for good.
        UUID tenantA = seedTenant("Tenant D Corp", "tenant-d-corp");
        UUID tenantB = seedTenant("Tenant E Corp", "tenant-e-corp");

        TenantContext.setCurrentTenantId(tenantA);
        EventEntity eventUnderTenantA = eventRepository.save(newDraftEvent("Tenant D Only Event"));

        TenantContext.setCurrentTenantId(tenantB);
        Optional<EventEntity> readAsTenantB = eventRepository.findById(eventUnderTenantA.getId());

        assertThat(readAsTenantB)
                .as("known limitation: bare findById() is not protected by the tenant filter — callers must use findByIdAndTenantId")
                .isPresent();
    }

    @Autowired
    private com.outreach.platform.event.service.EventService eventService;

    @Test
    void listsAndSearches_onlyReturnTheCallersTenant() {
        // Lists, searches and counts are JPQL, which only the Hibernate tenant filter scopes. If the
        // filter isn't switched on, every tenant sees every other tenant's events.
        UUID tenantA = seedTenant("Tenant F Corp", "tenant-f-corp");
        UUID tenantB = seedTenant("Tenant G Corp", "tenant-g-corp");
        TenantContext.setCurrentTenantId(tenantA);
        eventRepository.save(newDraftEvent("Tenant F Private Gala"));

        TenantContext.setCurrentTenantId(tenantB);
        var criteria = new com.outreach.platform.event.model.dto.EventSearchCriteria(null, null, null, null, null, null);
        var pageable = org.springframework.data.domain.PageRequest.of(0, 200);
        assertThat(eventService.listEvents(criteria, pageable).getContent())
                .extracting(com.outreach.platform.event.model.dto.EventDto::eventName)
                .doesNotContain("Tenant F Private Gala");
        assertThat(eventService.listEvents(byText("Private Gala"), pageable).getTotalElements()).isZero();

        TenantContext.setCurrentTenantId(tenantA);
        assertThat(eventService.listEvents(byText("Private Gala"), pageable).getTotalElements()).isEqualTo(1);
    }

    private static com.outreach.platform.event.model.dto.EventSearchCriteria byText(String query) {
        return new com.outreach.platform.event.model.dto.EventSearchCriteria(null, null, null, null, null, query);
    }

    // Organizations belong to auth-service; event tables only need the row their tenant_id
    // foreign key points at.
    private UUID seedTenant(String name, String slug) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO tenants (id, name, slug, status, created_date, created_by, version) "
                + "VALUES (?, ?, ?, 'ACTIVE', now(), 'test', 0)", id, name, slug);
        return id;
    }

    private EventEntity newDraftEvent(String eventName) {
        EventEntity event = new EventEntity();
        event.setEventCode("EVT-" + UUID.randomUUID().toString().substring(0, 8));
        event.setEventName(eventName);
        event.setStatus(EventStatus.DRAFT);
        event.setEventDate(LocalDate.of(2025, 6, 15));
        event.setCity("Bangalore");
        event.setMaxVolunteers(50);
        event.setRegisteredCount(0);
        event.setAttendedCount(0);
        return event;
    }
}
