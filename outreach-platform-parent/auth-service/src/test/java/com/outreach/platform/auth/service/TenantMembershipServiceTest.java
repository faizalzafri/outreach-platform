package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.Tenant;
import com.outreach.platform.auth.entity.TenantMembership;
import com.outreach.platform.auth.model.TenantRole;
import com.outreach.platform.auth.model.TenantStatus;
import com.outreach.platform.auth.repo.TenantMembershipRepository;
import com.outreach.platform.auth.repo.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TenantMembershipService}, focused on the multi-tenant login/selection
 * logic added alongside {@code POST /api/auth/select-tenant} — real OAuth2 user-token flows
 * aren't easily driven in this test suite (the registered clients only support authorization_code
 * and client_credentials, and only the former carries user context, which needs a browser/PKCE
 * dance to obtain), so this logic is verified at the service layer instead.
 */
@ExtendWith(MockitoExtension.class)
class TenantMembershipServiceTest {

    @Mock
    private TenantMembershipRepository tenantMembershipRepository;

    @Mock
    private TenantRepository tenantRepository;

    private TenantMembershipService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TenantMembershipService(tenantMembershipRepository, tenantRepository);
    }

    @Test
    @DisplayName("getActiveTenantForUser returns the first active membership when none was ever explicitly selected")
    void getActiveTenantForUser_noneSelected_returnsFirstActive() {
        TenantMembership membershipA = membership(TENANT_A, null);
        TenantMembership membershipB = membership(TENANT_B, null);
        when(tenantMembershipRepository.findByUserId(USER_ID)).thenReturn(List.of(membershipA, membershipB));
        when(tenantRepository.findById(TENANT_A)).thenReturn(Optional.of(tenant(TENANT_A, TenantStatus.ACTIVE)));

        Optional<Tenant> result = service.getActiveTenantForUser(USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(TENANT_A);
    }

    @Test
    @DisplayName("getActiveTenantForUser prefers the most recently explicitly selected tenant over the first membership")
    void getActiveTenantForUser_withSelection_prefersSelected() {
        TenantMembership membershipA = membership(TENANT_A, null);
        TenantMembership membershipB = membership(TENANT_B, Instant.parse("2026-09-01T00:00:00Z"));
        when(tenantMembershipRepository.findByUserId(USER_ID)).thenReturn(List.of(membershipA, membershipB));
        when(tenantRepository.findById(TENANT_B)).thenReturn(Optional.of(tenant(TENANT_B, TenantStatus.ACTIVE)));

        Optional<Tenant> result = service.getActiveTenantForUser(USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(TENANT_B);
    }

    @Test
    @DisplayName("isTenantSelectionRequired is true for a user with two active memberships and no prior selection")
    void isTenantSelectionRequired_multipleActiveNoneSelected_true() {
        when(tenantMembershipRepository.findByUserId(USER_ID))
                .thenReturn(List.of(membership(TENANT_A, null), membership(TENANT_B, null)));
        when(tenantRepository.findById(TENANT_A)).thenReturn(Optional.of(tenant(TENANT_A, TenantStatus.ACTIVE)));
        when(tenantRepository.findById(TENANT_B)).thenReturn(Optional.of(tenant(TENANT_B, TenantStatus.ACTIVE)));

        assertThat(service.isTenantSelectionRequired(USER_ID)).isTrue();
    }

    @Test
    @DisplayName("isTenantSelectionRequired is false once a selection has been made, even with multiple active memberships")
    void isTenantSelectionRequired_alreadySelected_false() {
        when(tenantMembershipRepository.findByUserId(USER_ID))
                .thenReturn(List.of(membership(TENANT_A, Instant.now()), membership(TENANT_B, null)));

        assertThat(service.isTenantSelectionRequired(USER_ID)).isFalse();
    }

    @Test
    @DisplayName("isTenantSelectionRequired is false for a single-tenant user")
    void isTenantSelectionRequired_singleTenant_false() {
        when(tenantMembershipRepository.findByUserId(USER_ID)).thenReturn(List.of(membership(TENANT_A, null)));
        when(tenantRepository.findById(TENANT_A)).thenReturn(Optional.of(tenant(TENANT_A, TenantStatus.ACTIVE)));

        assertThat(service.isTenantSelectionRequired(USER_ID)).isFalse();
    }

    @Test
    @DisplayName("selectTenant records the selection when the user has a membership in an active tenant")
    void selectTenant_validMembership_recordsSelection() {
        TenantMembership membershipB = membership(TENANT_B, null);
        when(tenantMembershipRepository.findByUserId(USER_ID))
                .thenReturn(List.of(membership(TENANT_A, null), membershipB));
        when(tenantRepository.findById(TENANT_B)).thenReturn(Optional.of(tenant(TENANT_B, TenantStatus.ACTIVE)));

        service.selectTenant(USER_ID, TENANT_B);

        assertThat(membershipB.getLastSelectedAt()).isNotNull();
        verify(tenantMembershipRepository).save(membershipB);
    }

    @Test
    @DisplayName("selectTenant rejects a tenant the user has no membership in")
    void selectTenant_noMembership_throws() {
        when(tenantMembershipRepository.findByUserId(USER_ID)).thenReturn(List.of(membership(TENANT_A, null)));

        assertThrows(TenantMembershipService.MembershipNotFoundException.class,
                () -> service.selectTenant(USER_ID, TENANT_B));
    }

    @Test
    @DisplayName("selectTenant rejects a suspended tenant even if the user is a member")
    void selectTenant_inactiveTenant_throws() {
        when(tenantMembershipRepository.findByUserId(USER_ID)).thenReturn(List.of(membership(TENANT_A, null)));
        when(tenantRepository.findById(TENANT_A)).thenReturn(Optional.of(tenant(TENANT_A, TenantStatus.SUSPENDED)));

        assertThrows(TenantMembershipService.TenantNotActiveException.class,
                () -> service.selectTenant(USER_ID, TENANT_A));
    }

    @Test
    @DisplayName("listActiveMembershipsForUser excludes non-active tenants")
    void listActiveMembershipsForUser_excludesInactive() {
        when(tenantMembershipRepository.findByUserId(USER_ID))
                .thenReturn(List.of(membership(TENANT_A, null), membership(TENANT_B, null)));
        when(tenantRepository.findById(TENANT_A)).thenReturn(Optional.of(tenant(TENANT_A, TenantStatus.ACTIVE)));
        when(tenantRepository.findById(TENANT_B)).thenReturn(Optional.of(tenant(TENANT_B, TenantStatus.SUSPENDED)));

        List<TenantMembershipService.TenantSummary> result = service.listActiveMembershipsForUser(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tenantId()).isEqualTo(TENANT_A);
    }

    private TenantMembership membership(UUID tenantId, Instant lastSelectedAt) {
        TenantMembership membership = new TenantMembership();
        membership.setTenantId(tenantId);
        membership.setUserId(USER_ID);
        membership.setRole(TenantRole.ADMIN);
        membership.setLastSelectedAt(lastSelectedAt);
        return membership;
    }

    private Tenant tenant(UUID id, TenantStatus status) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName("Tenant " + id);
        tenant.setSlug("tenant-" + id);
        tenant.setStatus(status);
        return tenant;
    }
}
