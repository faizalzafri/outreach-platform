package com.outreach.platform.gateway.filter;

import com.outreach.platform.common.tenant.TenantConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Extracts tenant ID from the JWT and propagates it as X-Tenant-ID header to downstream services. */
@Component
public class TenantExtractionFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantExtractionFilter.class);

    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String PLATFORM_ADMIN_CLAIM = "platform_admin";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> processAuthenticatedRequest(exchange, chain, auth))
                .switchIfEmpty(chain.filter(stripTenantHeader(exchange)));
    }

    private Mono<Void> processAuthenticatedRequest(ServerWebExchange exchange,
                                                    GatewayFilterChain chain,
                                                    JwtAuthenticationToken auth) {
        Jwt jwt = auth.getToken();

        boolean isPlatformAdmin = Boolean.TRUE.equals(jwt.getClaim(PLATFORM_ADMIN_CLAIM));

        if (isPlatformAdmin) {
            log.debug("Platform admin request — forwarding without X-Tenant-ID header");
            return chain.filter(stripTenantHeader(exchange));
        }

        String tenantIdClaim = jwt.getClaimAsString(TENANT_ID_CLAIM);

        if (tenantIdClaim == null || tenantIdClaim.isBlank()) {
            log.warn("JWT missing tenant_id claim for non-admin user: {}", jwt.getSubject());
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        UUID tenantId;
        try {
            tenantId = UUID.fromString(tenantIdClaim);
        } catch (IllegalArgumentException e) {
            log.warn("Malformed tenant_id UUID in JWT: {}", tenantIdClaim);
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(TenantConstants.X_TENANT_ID_HEADER);
                    headers.add(TenantConstants.X_TENANT_ID_HEADER, tenantId.toString());
                })
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        return chain.filter(mutatedExchange);
    }

    /** Strips any client-provided X-Tenant-ID header to prevent spoofing. */
    private ServerWebExchange stripTenantHeader(ServerWebExchange exchange) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(TenantConstants.X_TENANT_ID_HEADER))
                .build();

        return exchange.mutate()
                .request(mutatedRequest)
                .build();
    }

    @Override
    public int getOrder() {
        // Run after security filter but before routing
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
