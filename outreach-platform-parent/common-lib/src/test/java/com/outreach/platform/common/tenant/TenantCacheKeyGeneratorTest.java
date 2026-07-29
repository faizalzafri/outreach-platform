package com.outreach.platform.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantCacheKeyGenerator}.
 * Validates tenant-prefixed key generation and Platform_Admin (no context) fallback.
 */
class TenantCacheKeyGeneratorTest {

    private final TenantCacheKeyGenerator keyGenerator = new TenantCacheKeyGenerator();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Generate key with tenant context produces tenant-prefixed key")
    void generateWithTenantContext() throws Exception {
        UUID tenantId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        TenantContext.setCurrentTenantId(tenantId);

        Method method = SampleService.class.getMethod("findById", UUID.class);
        UUID paramId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        Object key = keyGenerator.generate(new SampleService(), method, paramId);

        int expectedHash = Arrays.deepHashCode(new Object[]{paramId});
        String expected = "tenant:" + tenantId + ":SampleService.findById:" + expectedHash;
        assertThat(key).isEqualTo(expected);
    }

    @Test
    @DisplayName("Generate key without tenant context omits tenant prefix")
    void generateWithoutTenantContext() throws Exception {
        // No TenantContext set — simulates Platform_Admin cross-tenant mode
        Method method = SampleService.class.getMethod("findById", UUID.class);
        UUID paramId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        Object key = keyGenerator.generate(new SampleService(), method, paramId);

        int expectedHash = Arrays.deepHashCode(new Object[]{paramId});
        String expected = "SampleService.findById:" + expectedHash;
        assertThat(key).isEqualTo(expected);
    }

    @Test
    @DisplayName("Generate key with no parameters produces stable key")
    void generateWithNoParams() throws Exception {
        UUID tenantId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        TenantContext.setCurrentTenantId(tenantId);

        Method method = SampleService.class.getMethod("findAll");

        Object key = keyGenerator.generate(new SampleService(), method);

        int expectedHash = Arrays.deepHashCode(new Object[]{});
        String expected = "tenant:" + tenantId + ":SampleService.findAll:" + expectedHash;
        assertThat(key).isEqualTo(expected);
    }

    @Test
    @DisplayName("Generate key with multiple parameters includes all in hash")
    void generateWithMultipleParams() throws Exception {
        UUID tenantId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        TenantContext.setCurrentTenantId(tenantId);

        Method method = SampleService.class.getMethod("search", String.class, int.class);

        Object key = keyGenerator.generate(new SampleService(), method, "query", 10);

        int expectedHash = Arrays.deepHashCode(new Object[]{"query", 10});
        String expected = "tenant:" + tenantId + ":SampleService.search:" + expectedHash;
        assertThat(key).isEqualTo(expected);
    }

    @Test
    @DisplayName("Different tenants produce different cache keys for same method and params")
    void differentTenantsProduceDifferentKeys() throws Exception {
        Method method = SampleService.class.getMethod("findAll");

        TenantContext.setCurrentTenantId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        Object keyA = keyGenerator.generate(new SampleService(), method);

        TenantContext.clear();
        TenantContext.setCurrentTenantId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        Object keyB = keyGenerator.generate(new SampleService(), method);

        assertThat(keyA).isNotEqualTo(keyB);
        assertThat(keyA.toString()).startsWith("tenant:aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa:");
        assertThat(keyB.toString()).startsWith("tenant:bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb:");
    }

    /**
     * Dummy service class used as the target for key generation tests.
     */
    public static class SampleService {
        public Object findById(UUID id) { return null; }
        public Object findAll() { return null; }
        public Object search(String query, int limit) { return null; }
    }
}
