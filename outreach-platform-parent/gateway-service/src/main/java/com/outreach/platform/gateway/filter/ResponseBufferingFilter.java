package com.outreach.platform.gateway.filter;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Buffers every proxied response body into a single DataBuffer and writes it with an
 * explicit Content-Length instead of streaming it with Transfer-Encoding: chunked.
 *
 * Diagnosed via real browser testing plus a raw TCP socket capture of the literal bytes
 * on the wire: this Spring Cloud Gateway / Reactor Netty version never writes the
 * terminating zero-length chunk when relaying a proxied response — the capture showed
 * exactly one data chunk followed by nothing (no {@code 0\r\n\r\n}), on every route and
 * every status code tried. curl tolerates the malformed stream (with a warning, since it
 * still received the actual bytes); Node's http client (used by Vite's dev proxy) and
 * the browser's own fetch do not, and hang indefinitely waiting for a terminator that
 * will never arrive — confirmed both hung the same way even after ruling out every other
 * candidate (a response-header-mutation-timing bug, the CircuitBreaker default filter,
 * Reactor Netty's connection pool, and HTTP keep-alive) one at a time against the real
 * running stack. Buffering the whole body and sending it as one Content-Length-framed
 * write sidesteps the chunked-encoding path entirely — every response this gateway
 * proxies is a small JSON payload, so this has no meaningful memory or latency cost.
 */
@Component
public class ResponseBufferingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(body)
                        .flatMap(buffer -> {
                            setContentLengthHeader(buffer.readableByteCount());
                            return super.writeWith(Mono.just(buffer));
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            setContentLengthHeader(0);
                            return super.setComplete();
                        }));
            }

            /**
             * A response the gateway's own security filter chain writes directly — a 401 from
             * an expired/invalid JWT, before any proxying happens — carries {@link
             * org.springframework.http.ReadOnlyHttpHeaders}, which throws on any mutation. That
             * kind of response is already small, complete, and not streamed through the
             * Reactor Netty proxy path this filter works around, so there's nothing to fix;
             * skip the header rewrite rather than crash the exchange.
             */
            private void setContentLengthHeader(long length) {
                try {
                    getHeaders().remove(HttpHeaders.TRANSFER_ENCODING);
                    getHeaders().setContentLength(length);
                } catch (UnsupportedOperationException ignored) {
                    // read-only headers — see method javadoc
                }
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        // Must wrap the actual response write, so run as one of the outermost filters.
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
