package com.outreach.platform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();

        // beforeCommit is the purpose-built hook for mutating response headers safely, no matter
        // what triggers the commit (writeWith, writeAndFlushWith, or setComplete for an empty body) —
        // safer than the previous implementation, which mutated headers inside a
        // ServerHttpResponseDecorator's writeWith() override instead, at the moment the body write
        // begins, by which point the response may already be committed.
        //
        // Also forces `Connection: close` on every response. Diagnosed via real browser testing:
        // every proxied response (any route, any status) hung indefinitely for HTTP clients that
        // hold connections open for reuse (Node's http client — used by Vite's dev proxy — and the
        // browser's own fetch), even though the bytes on the wire were complete and valid (confirmed
        // with curl and by inspecting the raw response). Proxying straight to a backing service
        // instead of through the gateway worked cleanly on the exact same client, and every backing
        // service sends `Connection: close` (Tomcat's default) — the gateway's own Netty server was
        // the only hop keeping connections alive without ever signalling completion on them to a
        // client waiting to reuse one. Sending the same header the backing services already send
        // resolves it without needing to chase the underlying keep-alive framing bug in Reactor
        // Netty itself.
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            headers.addIfAbsent(CORRELATION_ID_HEADER, finalCorrelationId);
            headers.set(HttpHeaders.CONNECTION, "close");
            return Mono.empty();
        });

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
