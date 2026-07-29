package com.outreach.platform.common.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TenantCacheInvalidationService}.
 * Validates key tracking via SADD and bulk invalidation via SMEMBERS → pipeline DELETE → DEL tracking set.
 */
@ExtendWith(MockitoExtension.class)
class TenantCacheInvalidationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private RedisKeyCommands keyCommands;

    private TenantCacheInvalidationService service;

    private static final UUID TENANT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String TRACKING_KEY = "tenant:550e8400-e29b-41d4-a716-446655440000:_keys";

    @BeforeEach
    void setUp() {
        service = new TenantCacheInvalidationService(redisTemplate);
    }

    @Test
    @DisplayName("trackKey adds cache key to tenant's tracking Set via SADD")
    void trackKeyAddsToCacheSet() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        String cacheKey = "tenant:550e8400-e29b-41d4-a716-446655440000:EventService.findById:12345";
        service.trackKey(TENANT_ID, cacheKey);

        verify(setOperations).add(TRACKING_KEY, cacheKey);
    }

    @Test
    @DisplayName("trackKey with null tenantId does nothing")
    void trackKeyWithNullTenantIdIsNoOp() {
        service.trackKey(null, "some-key");

        verifyNoInteractions(setOperations);
    }

    @Test
    @DisplayName("trackKey with null cacheKey does nothing")
    void trackKeyWithNullCacheKeyIsNoOp() {
        service.trackKey(TENANT_ID, null);

        verify(redisTemplate, never()).opsForSet();
    }

    @Test
    @DisplayName("trackKey with blank cacheKey does nothing")
    void trackKeyWithBlankCacheKeyIsNoOp() {
        service.trackKey(TENANT_ID, "   ");

        verify(redisTemplate, never()).opsForSet();
    }

    @Test
    @DisplayName("invalidateAllForTenant reads members, deletes keys in pipeline, deletes tracking set")
    void invalidateAllForTenantDeletesTrackedKeys() {
        Set<String> trackedKeys = Set.of(
                "tenant:550e8400-e29b-41d4-a716-446655440000:EventService.findById:111",
                "tenant:550e8400-e29b-41d4-a716-446655440000:EventService.findAll:222"
        );

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(TRACKING_KEY)).thenReturn(trackedKeys);

        // Mock pipeline execution
        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            when(redisConnection.keyCommands()).thenReturn(keyCommands);
            callback.doInRedis(redisConnection);
            return Collections.emptyList();
        });

        when(redisTemplate.delete(TRACKING_KEY)).thenReturn(true);

        long result = service.invalidateAllForTenant(TENANT_ID);

        assertThat(result).isEqualTo(2);
        verify(setOperations).members(TRACKING_KEY);
        verify(redisTemplate).executePipelined(any(RedisCallback.class));
        verify(redisTemplate).delete(TRACKING_KEY);

        // Verify each tracked key was deleted in the pipeline
        verify(keyCommands, times(2)).del(any(byte[].class));
    }

    @Test
    @DisplayName("invalidateAllForTenant with null tenantId returns 0")
    void invalidateWithNullTenantIdReturnsZero() {
        long result = service.invalidateAllForTenant(null);

        assertThat(result).isZero();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("invalidateAllForTenant with empty tracking set returns 0 and cleans up set")
    void invalidateWithEmptyTrackingSetReturnsZero() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(TRACKING_KEY)).thenReturn(Collections.emptySet());
        when(redisTemplate.delete(TRACKING_KEY)).thenReturn(true);

        long result = service.invalidateAllForTenant(TENANT_ID);

        assertThat(result).isZero();
        verify(redisTemplate).delete(TRACKING_KEY);
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    @DisplayName("invalidateAllForTenant with null members from Redis returns 0 and cleans up set")
    void invalidateWithNullMembersReturnsZero() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(TRACKING_KEY)).thenReturn(null);
        when(redisTemplate.delete(TRACKING_KEY)).thenReturn(true);

        long result = service.invalidateAllForTenant(TENANT_ID);

        assertThat(result).isZero();
        verify(redisTemplate).delete(TRACKING_KEY);
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }
}
