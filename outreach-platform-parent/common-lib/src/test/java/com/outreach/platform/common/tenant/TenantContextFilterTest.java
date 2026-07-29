package com.outreach.platform.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TenantContextFilter}.
 * Validates header extraction, UUID validation, context population, and cleanup.
 */
class TenantContextFilterTest {

    private final TenantContextFilter filter = new TenantContextFilter();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain filterChain = mock(FilterChain.class);

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Valid UUID header populates TenantContext during request processing")
    void validHeader_populatesTenantContext() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        request.addHeader(TenantConstants.X_TENANT_ID_HEADER, tenantId.toString());

        // Capture the TenantContext state during filter chain execution
        doAnswer(invocation -> {
            assertThat(TenantContext.isPresent()).isTrue();
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(tenantId);
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("TenantContext is cleared after successful request processing")
    void tenantContext_clearedAfterSuccess() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        request.addHeader(TenantConstants.X_TENANT_ID_HEADER, tenantId.toString());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(TenantContext.isPresent()).isFalse();
    }

    @Test
    @DisplayName("TenantContext is cleared even when filter chain throws exception")
    void tenantContext_clearedAfterException() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        request.addHeader(TenantConstants.X_TENANT_ID_HEADER, tenantId.toString());
        doThrow(new ServletException("simulated failure")).when(filterChain).doFilter(request, response);

        try {
            filter.doFilterInternal(request, response, filterChain);
        } catch (ServletException ignored) {
            // expected
        }

        assertThat(TenantContext.isPresent()).isFalse();
    }

    @Test
    @DisplayName("Invalid UUID header returns HTTP 400 with INVALID_TENANT_ID error")
    void invalidUuidHeader_returns400() throws ServletException, IOException {
        request.addHeader(TenantConstants.X_TENANT_ID_HEADER, "not-a-valid-uuid");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("INVALID_TENANT_ID");
        assertThat(response.getContentAsString()).contains("X-Tenant-ID header must be a valid UUID");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Missing header passes through without setting TenantContext")
    void missingHeader_passesThrough() throws ServletException, IOException {
        // No X-Tenant-ID header added

        doAnswer(invocation -> {
            assertThat(TenantContext.isPresent()).isFalse();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Blank header passes through without setting TenantContext")
    void blankHeader_passesThrough() throws ServletException, IOException {
        request.addHeader(TenantConstants.X_TENANT_ID_HEADER, "   ");

        doAnswer(invocation -> {
            assertThat(TenantContext.isPresent()).isFalse();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Header with leading/trailing whitespace is trimmed and accepted")
    void headerWithWhitespace_trimmedAndAccepted() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        request.addHeader(TenantConstants.X_TENANT_ID_HEADER, "  " + tenantId.toString() + "  ");

        doAnswer(invocation -> {
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(tenantId);
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Default Tenant ID in header is accepted")
    void defaultTenantId_isAccepted() throws ServletException, IOException {
        request.addHeader(TenantConstants.X_TENANT_ID_HEADER,
                TenantConstants.DEFAULT_TENANT_ID.toString());

        doAnswer(invocation -> {
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantConstants.DEFAULT_TENANT_ID);
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
