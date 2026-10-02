package com.outreach.platform.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** On a cache miss the gateway asks auth-service; the load balancer stands in for the network. */
class TenantStatusServiceTest {

    private final ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
    // Real default methods (apply/andThen) so WebClient can chain it; filter() is stubbed per test.
    private final ReactorLoadBalancerExchangeFilterFunction authService = mock(
            ReactorLoadBalancerExchangeFilterFunction.class, org.mockito.Mockito.CALLS_REAL_METHODS);
    private TenantStatusService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn(Mono.empty());
        when(values.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        service = new TenantStatusService(redis, authService, "http://auth-service/internal/tenants/{id}/status");
    }

    @Test
    void cacheMiss_usesAuthServicesAnswer_andCachesIt() {
        org.mockito.Mockito.doReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"status\":\"SUSPENDED\"}").build())).when(authService).filter(any(), any());

        assertThat(service.getTenantStatus("t-1").block()).isEqualTo("SUSPENDED");
        verify(values).set(eq("tenant:status:t-1"), eq("SUSPENDED"), any(Duration.class));

        // Second call is answered from the local cache.
        assertThat(service.getTenantStatus("t-1").block()).isEqualTo("SUSPENDED");
        verify(authService, times(1)).filter(any(), any());
    }

    @Test
    void authServiceUnreachable_letsTrafficThrough_withoutCachingTheGuess() {
        org.mockito.Mockito.doReturn(Mono.error(new IllegalStateException("no instances"))).when(authService).filter(any(), any());

        assertThat(service.getTenantStatus("t-2").block()).isEqualTo(TenantStatusService.STATUS_ACTIVE);
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
