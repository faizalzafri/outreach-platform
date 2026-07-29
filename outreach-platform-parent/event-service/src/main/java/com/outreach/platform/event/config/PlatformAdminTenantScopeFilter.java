package com.outreach.platform.event.config;

import com.outreach.platform.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that supports optional {@code ?tenantId={uuid}} query parameter
 * for Platform_Admin users to scope their requests to a specific tenant.
 * <p>
 * When a Platform_Admin includes a valid {@code tenantId} query parameter, this filter
 * sets the {@link TenantContext} so downstream services (repositories, caches, etc.)
 * operate within that tenant's scope. Without the parameter, Platform_Admins operate
 * in cross-tenant mode (no TenantContext set, filter disabled by {@code TenantFilterAspect}).
 * <p>
 * This filter runs after the Spring Security filter chain (requires authentication
 * to be established) but before business controllers execute.
 * <p>
 * Non-Platform_Admin users are unaffected by this filter — their TenantContext is
 * already set by the standard {@code TenantContextFilter} from common-lib.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class PlatformAdminTenantScopeFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminTenantScopeFilter.class);
    private static final String TENANT_ID_PARAM = "tenantId";
    private static final String PLATFORM_ADMIN_AUTHORITY = "ROLE_PLATFORM_ADMIN";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (isPlatformAdmin()) {
                String tenantIdParam = request.getParameter(TENANT_ID_PARAM);
                if (tenantIdParam != null && !tenantIdParam.isBlank()) {
                    UUID tenantId = parseUuid(tenantIdParam, response);
                    if (tenantId == null) {
                        return; // response already written with 400
                    }
                    TenantContext.setCurrentTenantId(tenantId);
                    log.debug("Platform_Admin scoped to tenant: {}", tenantId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // Only clear if we were the ones who set it (Platform_Admin with tenantId param)
            // The standard TenantContextFilter handles cleanup for regular users
            if (isPlatformAdmin() && request.getParameter(TENANT_ID_PARAM) != null) {
                TenantContext.clear();
            }
        }
    }

    private boolean isPlatformAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PLATFORM_ADMIN_AUTHORITY::equals);
    }

    private UUID parseUuid(String value, HttpServletResponse response) throws IOException {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"INVALID_TENANT_ID\",\"message\":\"tenantId query parameter must be a valid UUID\"}"
            );
            return null;
        }
    }
}
