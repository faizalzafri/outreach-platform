package com.outreach.platform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

/**
 * Global filter that enforces differentiated rate limiting:
 * - Authenticated users: 100 req/min (handled by route-level RequestRateLimiter)
 * - Unauthenticated IPs: 20 req/min (enforced here via Redis)
 *
 * This filter only activates for unauthenticated requests, complementing
 * the per-route RequestRateLimiter which handles authenticated traffic.
 */
@Component
public class AdaptiveRateLimitFilter implements GlobalFilter, Ordered {

    private static final int UNAUTHENTICATED_RATE_LIMIT = 20;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);
    private static final String KEY_PREFIX = "rate_limit:unauth:";

    private final ReactiveStringRedisTemplate redisTemplate;

    public AdaptiveRateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .flatMap(principal -> chain.filter(exchange))
                .switchIfEmpty(applyUnauthenticatedRateLimit(exchange, chain));
    }

    private Mono<Void> applyUnauthenticatedRateLimit(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = Objects.requireNonNull(
                exchange.getRequest().getRemoteAddress(),
                "Remote address must not be null"
        ).getAddress().getHostAddress();

        String redisKey = KEY_PREFIX + clientIp;

        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count == 1L) {
                        return redisTemplate.expire(redisKey, WINDOW_DURATION)
                                .then(chain.filter(exchange));
                    }
                    if (count > UNAUTHENTICATED_RATE_LIMIT) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
                                String.valueOf(UNAUTHENTICATED_RATE_LIMIT));
                        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        // Execute after correlation ID filter but before routing
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
