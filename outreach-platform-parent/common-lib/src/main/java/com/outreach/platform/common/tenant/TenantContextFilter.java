package com.outreach.platform.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that extracts the tenant identifier from the {@code X-Tenant-ID} request header
 * and populates the {@link TenantContext} thread-local for the duration of the request.
 * <p>
 * Behavior:
 * <ul>
 *   <li>If the header is present and contains a valid UUID, the tenant context is set.</li>
 *   <li>If the header is present but contains an invalid UUID format, the request is rejected
 *       with HTTP 400 and error code {@code INVALID_TENANT_ID}.</li>
 *   <li>If the header is absent, the filter passes through without setting context.
 *       Endpoints that require tenant scoping are protected separately by
 *       {@code TenantFilterAspect} at the repository layer.</li>
 * </ul>
 * <p>
 * The {@link TenantContext} is <strong>always</strong> cleared in a {@code finally} block
 * to prevent thread-local leakage in servlet container thread pools.
 *
 * @see TenantContext
 * @see TenantConstants#X_TENANT_ID_HEADER
 */
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private static final String ERROR_CODE_INVALID_TENANT_ID = "INVALID_TENANT_ID";
    private static final String ERROR_MESSAGE_INVALID_UUID = "X-Tenant-ID header must be a valid UUID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantIdHeader = request.getHeader(TenantConstants.X_TENANT_ID_HEADER);

            if (tenantIdHeader != null && !tenantIdHeader.isBlank()) {
                UUID tenantId = parseUuid(tenantIdHeader.trim());

                if (tenantId == null) {
                    log.warn("Received invalid X-Tenant-ID header value: {}", tenantIdHeader);
                    writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                            ERROR_CODE_INVALID_TENANT_ID, ERROR_MESSAGE_INVALID_UUID);
                    return;
                }

                TenantContext.setCurrentTenantId(tenantId);
                log.debug("Tenant context set to: {}", tenantId);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Attempts to parse the given string as a UUID.
     *
     * @param value the string to parse
     * @return the parsed UUID, or {@code null} if the format is invalid
     */
    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Writes a JSON error response conforming to the platform's standardized error format.
     */
    private void writeErrorResponse(HttpServletResponse response, int status,
                                    String errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"fieldErrors\":[]}",
                java.time.Instant.now().toString(),
                status,
                errorCode,
                message
        );

        response.getWriter().write(body);
        response.getWriter().flush();
    }
}
