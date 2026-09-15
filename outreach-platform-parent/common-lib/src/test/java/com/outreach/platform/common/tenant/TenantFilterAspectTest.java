package com.outreach.platform.common.tenant;

import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TenantFilterAspect}.
 * Validates Hibernate filter enablement, platform admin bypass, and missing context handling.
 */
class TenantFilterAspectTest {

    private EntityManager entityManager;
    private Session session;
    private Filter filter;
    private TenantFilterAspect aspect;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        session = mock(Session.class);
        filter = mock(Filter.class);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(TenantConstants.TENANT_FILTER_NAME)).thenReturn(filter);
        when(filter.setParameter(eq("tenantId"), any(UUID.class))).thenReturn(filter);

        aspect = new TenantFilterAspect(entityManager);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Enables Hibernate tenant filter with current tenant ID from TenantContext")
    void enablesTenantFilter_whenContextPresent() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenantId(tenantId);
        setAuthentication("user", "ROLE_PMO");

        aspect.enableTenantFilter();

        verify(session).enableFilter(TenantConstants.TENANT_FILTER_NAME);
        verify(filter).setParameter("tenantId", tenantId);
    }

    @Test
    @DisplayName("Skips tenant filter enablement for ROLE_PLATFORM_ADMIN")
    void skipsFilter_whenPlatformAdmin() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenantId(tenantId);
        setAuthentication("admin", "ROLE_PLATFORM_ADMIN");

        aspect.enableTenantFilter();

        verify(session, never()).enableFilter(any());
        verify(entityManager, never()).unwrap(any());
    }

    @Test
    @DisplayName("Throws IllegalStateException when TenantContext is empty and user is not admin")
    void throwsException_whenContextEmpty_andNotAdmin() {
        setAuthentication("user", "ROLE_PMO");

        assertThatThrownBy(() -> aspect.enableTenantFilter())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tenant filter expected but TenantContext is empty");
    }

    @Test
    @DisplayName("Throws IllegalStateException when no authentication present and context empty")
    void throwsException_whenNoAuthentication_andContextEmpty() {
        // No SecurityContext set, no TenantContext set

        assertThatThrownBy(() -> aspect.enableTenantFilter())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tenant filter expected but TenantContext is empty");
    }

    @Test
    @DisplayName("Platform admin with multiple roles still bypasses filter")
    void platformAdminWithMultipleRoles_bypassesFilter() {
        TenantContext.setCurrentTenantId(UUID.randomUUID());
        setAuthentication("admin", "ROLE_PLATFORM_ADMIN", "ROLE_PMO");

        aspect.enableTenantFilter();

        verify(session, never()).enableFilter(any());
    }

    @Test
    @DisplayName("Non-admin user with context present enables filter successfully")
    void nonAdminUser_withContext_enablesFilter() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenantId(tenantId);
        setAuthentication("user", "ROLE_ADMIN", "ROLE_POC");

        aspect.enableTenantFilter();

        verify(session).enableFilter(TenantConstants.TENANT_FILTER_NAME);
        verify(filter).setParameter("tenantId", tenantId);
    }

    private void setAuthentication(String principal, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                principal, null, authorities);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
