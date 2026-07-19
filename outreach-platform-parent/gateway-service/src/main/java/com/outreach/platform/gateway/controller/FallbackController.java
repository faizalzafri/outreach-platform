package com.outreach.platform.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping
    public Mono<Map<String, Object>> getFallback(ServerWebExchange exchange) {
        return buildFallbackResponse(exchange);
    }

    @PostMapping
    public Mono<Map<String, Object>> postFallback(ServerWebExchange exchange) {
        return buildFallbackResponse(exchange);
    }

    @PutMapping
    public Mono<Map<String, Object>> putFallback(ServerWebExchange exchange) {
        return buildFallbackResponse(exchange);
    }

    @DeleteMapping
    public Mono<Map<String, Object>> deleteFallback(ServerWebExchange exchange) {
        return buildFallbackResponse(exchange);
    }

    @PatchMapping
    public Mono<Map<String, Object>> patchFallback(ServerWebExchange exchange) {
        return buildFallbackResponse(exchange);
    }

    private Mono<Map<String, Object>> buildFallbackResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = Map.of(
                "status", 503,
                "error", "Service Unavailable",
                "message", "The requested service is temporarily unavailable. Please try again later.",
                "timestamp", Instant.now().toString(),
                "path", exchange.getRequest().getPath().value()
        );

        return Mono.just(errorResponse);
    }
}
