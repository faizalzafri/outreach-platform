package com.outreach.platform.gateway.filter;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.gateway.service.TenantStatusService;

import static com.outreach.platform.gateway.service.TenantStatusService.STATUS_DEACTIVATED;
import static com.outreach.platform.gateway.service.TenantStatusService.STATUS_SUSPENDED;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

/** Validates the tenant's lifecycle status and rejects requests for deactivated or write-suspended tenants. */
@Component
public class TenantStatusValidationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantStatusValidationFilter.class);

    private static final Set<HttpMethod> WRITE_METHODS = Set.of(
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE
    );

    private final TenantStatusService tenantStatusService;

    public TenantStatusValidationFilter(TenantStatusService tenantStatusService) {
        this.tenantStatusService = tenantStatusService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantId = exchange.getRequest().getHeaders()
                .getFirst(TenantConstants.X_TENANT_ID_HEADER);

        // No X-Tenant-ID means Platform_Admin or unauthenticated — skip validation
        if (tenantId == null || tenantId.isBlank()) {
            return chain.filter(exchange);
        }

        return tenantStatusService.getTenantStatus(tenantId)
                .flatMap(status -> evaluateStatus(exchange, chain, tenantId, status))
                .onErrorResume(ex -> {
                    log.error("Failed to resolve tenant status for {}: {}", tenantId, ex.getMessage());
                    // Fail-open for availability
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> evaluateStatus(ServerWebExchange exchange,
                                       GatewayFilterChain chain,
                                       String tenantId,
                                       String status) {
        if (STATUS_DEACTIVATED.equals(status)) {
            log.warn("Rejecting request for deactivated tenant: {}", tenantId);
            return rejectWithError(exchange, HttpStatus.FORBIDDEN,
                    "TENANT_DEACTIVATED",
                    "Tenant " + tenantId + " is deactivated");
        }

        if (STATUS_SUSPENDED.equals(status)) {
            HttpMethod method = exchange.getRequest().getMethod();
            if (WRITE_METHODS.contains(method)) {
                log.warn("Rejecting write request ({}) for suspended tenant: {}", method, tenantId);
                return rejectWithError(exchange, HttpStatus.FORBIDDEN,
                        "TENANT_SUSPENDED",
                        "Tenant " + tenantId + " is suspended; write operations not allowed");
            }
            log.debug("Allowing read request for suspended tenant: {}", tenantId);
        }

        return chain.filter(exchange);
    }

    /** Writes a JSON error response in the platform's standardized format. */
    private Mono<Void> rejectWithError(ServerWebExchange exchange,
                                        HttpStatus httpStatus,
                                        String errorCode,
                                        String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String correlationId = exchange.getRequest().getHeaders()
                .getFirst("X-Correlation-ID");

        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"correlationId\":\"%s\",\"fieldErrors\":{}}",
                Instant.now().toString(),
                httpStatus.value(),
                errorCode,
                message,
                correlationId != null ? correlationId : ""
        );

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run after TenantExtractionFilter (+100)
        return Ordered.HIGHEST_PRECEDENCE + 101;
    }
}
