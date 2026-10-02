package com.outreach.platform.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public Mono<Map<String, Object>> fallback(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return Mono.just(Map.of(
                "status", 503,
                "error", "Service Unavailable",
                "message", "The requested service is temporarily unavailable. Please try again later.",
                "timestamp", Instant.now().toString(),
                "path", exchange.getRequest().getPath().value()
        ));
    }
}
