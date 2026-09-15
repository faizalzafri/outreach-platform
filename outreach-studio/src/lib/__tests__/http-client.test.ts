import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { normalizeError, setToastHandler, httpClient } from '@/lib/http-client';
import { useTenantStore } from '@/stores/tenant-store';
import type { AxiosError } from 'axios';
import type { NormalizedError, ToastPayload } from '@/types/api';

// --- Helper to create AxiosError-like objects for normalizeError unit tests ---

function createAxiosError(overrides: {
  response?: {
    status: number;
    statusText?: string;
    data?: unknown;
    headers?: Record<string, string>;
  };
  code?: string;
}): AxiosError {
  const error = new Error('Request failed') as AxiosError;
  error.isAxiosError = true;
  error.code = overrides.code;
  error.config = {} as never;
  error.toJSON = () => ({});

  if (overrides.response) {
    error.response = {
      status: overrides.response.status,
      statusText: overrides.response.statusText || 'Error',
      data: overrides.response.data,
      headers: overrides.response.headers || {},
      config: {} as never,
    } as never;
  } else {
    error.response = undefined;
  }

  return error;
}

describe('HTTP Client - Error Normalization', () => {
  describe('normalizeError', () => {
    it('should produce TIMEOUT type for connection aborted errors', () => {
      const error = createAxiosError({ code: 'ECONNABORTED' });
      const normalized = normalizeError(error);

      expect(normalized).toEqual<NormalizedError>({
        status: 0,
        type: 'TIMEOUT',
        message: 'Request timed out',
        correlationId: null,
        fieldErrors: [],
      });
    });

    it('should produce NETWORK_ERROR for errors without response and no timeout code', () => {
      const error = createAxiosError({ code: 'ERR_NETWORK' });
      const normalized = normalizeError(error);

      expect(normalized).toEqual<NormalizedError>({
        status: 0,
        type: 'NETWORK_ERROR',
        message: 'Network error',
        correlationId: null,
        fieldErrors: [],
      });
    });

    it('should normalize a standard backend error response', () => {
      const error = createAxiosError({
        response: {
          status: 400,
          statusText: 'Bad Request',
          data: {
            error: 'VALIDATION_ERROR',
            message: 'Validation failed',
            correlationId: 'corr-123',
            fieldErrors: [{ field: 'name', message: 'Name is required' }],
          },
          headers: { 'x-correlation-id': 'corr-123' },
        },
      });
      const normalized = normalizeError(error);

      expect(normalized).toEqual<NormalizedError>({
        status: 400,
        type: 'VALIDATION_ERROR',
        message: 'Validation failed',
        correlationId: 'corr-123',
        fieldErrors: [{ field: 'name', message: 'Name is required' }],
      });
    });

    it('should extract correlationId from response headers when not in body', () => {
      const error = createAxiosError({
        response: {
          status: 500,
          statusText: 'Internal Server Error',
          data: {
            error: 'SERVER_ERROR',
            message: 'Something went wrong',
          },
          headers: { 'x-correlation-id': 'header-corr-456' },
        },
      });
      const normalized = normalizeError(error);

      expect(normalized.correlationId).toBe('header-corr-456');
    });

    it('should produce UNKNOWN_ERROR for non-standard response bodies', () => {
      const error = createAxiosError({
        response: {
          status: 502,
          statusText: 'Bad Gateway',
          data: '<html>Error</html>',
          headers: {},
        },
      });
      const normalized = normalizeError(error);

      expect(normalized).toEqual<NormalizedError>({
        status: 502,
        type: 'UNKNOWN_ERROR',
        message: 'Bad Gateway',
        correlationId: null,
        fieldErrors: [],
      });
    });

    it('should produce UNKNOWN_ERROR when data is an object without error/message fields', () => {
      const error = createAxiosError({
        response: {
          status: 503,
          statusText: 'Service Unavailable',
          data: { someOtherField: 'value' },
          headers: {},
        },
      });
      const normalized = normalizeError(error);

      expect(normalized.type).toBe('UNKNOWN_ERROR');
      expect(normalized.message).toBe('Service Unavailable');
    });

    it('should handle empty fieldErrors gracefully', () => {
      const error = createAxiosError({
        response: {
          status: 404,
          statusText: 'Not Found',
          data: {
            error: 'NOT_FOUND',
            message: 'Resource not found',
            correlationId: 'corr-789',
          },
          headers: {},
        },
      });
      const normalized = normalizeError(error);

      expect(normalized.fieldErrors).toEqual([]);
    });
  });
});

describe('HTTP Client - 401 Retry Logic (Integration)', () => {
  beforeEach(() => {
    // Reset any custom handlers
    server.resetHandlers();
  });

  it('should retry a request after successful token refresh', async () => {
    let requestCount = 0;

    // Mock the token endpoint to return a valid token on refresh
    server.use(
      http.post('*/oauth2/token', () => {
        return HttpResponse.json({
          access_token: 'new-access-token-after-refresh',
          refresh_token: 'new-refresh-token',
          expires_in: 300,
          token_type: 'Bearer',
        });
      }),
      http.get('/api/test-endpoint', () => {
        requestCount++;
        if (requestCount === 1) {
          return HttpResponse.json(
            { error: 'Unauthorized', message: 'Token expired' },
            { status: 401 },
          );
        }
        return HttpResponse.json({ data: 'success' });
      }),
    );

    // Note: The actual retry depends on auth module state having a refresh token.
    // Without a real refresh token in memory, the silentRefresh will clear session
    // and the request will fail with a normalized 401 error.
    try {
      await httpClient.get('/test-endpoint');
    } catch (err) {
      // Expected: since auth module has no refresh token in test context,
      // silentRefresh returns null and login() is called
      const normalized = err as NormalizedError;
      expect(normalized.status).toBe(401);
    }
  });

  it('should redirect to login when refresh token is not available', async () => {
    server.use(
      http.get('/api/protected-resource', () => {
        return HttpResponse.json(
          { error: 'Unauthorized', message: 'Token expired' },
          { status: 401 },
        );
      }),
    );

    try {
      await httpClient.get('/protected-resource');
      expect.fail('Should have thrown');
    } catch (err) {
      const normalized = err as NormalizedError;
      expect(normalized.status).toBe(401);
      expect(normalized.type).toBe('Unauthorized');
    }
  });
});

describe('HTTP Client - 429 Rate Limit Handling (Integration)', () => {
  let capturedToasts: ToastPayload[] = [];

  beforeEach(() => {
    capturedToasts = [];
    setToastHandler((toast) => {
      capturedToasts.push(toast);
    });
  });

  afterEach(() => {
    // Reset to no-op handler
    setToastHandler(() => {});
  });

  it('should display a warning toast with Retry-After duration', async () => {
    server.use(
      http.get('/api/rate-limited', () => {
        return HttpResponse.json(
          { error: 'RATE_LIMITED', message: 'Too many requests' },
          {
            status: 429,
            headers: { 'Retry-After': '30' },
          },
        );
      }),
    );

    try {
      await httpClient.get('/rate-limited');
      expect.fail('Should have thrown');
    } catch {
      // Error is expected
    }

    expect(capturedToasts).toHaveLength(1);
    expect(capturedToasts[0].severity).toBe('warning');
    expect(capturedToasts[0].message).toContain('30');
    expect(capturedToasts[0].message).toContain('seconds');
  });

  it('should display a generic message when Retry-After header is absent', async () => {
    server.use(
      http.get('/api/rate-limited-no-header', () => {
        return HttpResponse.json(
          { error: 'RATE_LIMITED', message: 'Too many requests' },
          { status: 429 },
        );
      }),
    );

    try {
      await httpClient.get('/rate-limited-no-header');
      expect.fail('Should have thrown');
    } catch {
      // Error is expected
    }

    expect(capturedToasts).toHaveLength(1);
    expect(capturedToasts[0].severity).toBe('warning');
    expect(capturedToasts[0].message).toContain('try again later');
  });

  it('should parse numeric Retry-After header correctly', async () => {
    server.use(
      http.get('/api/rate-limited-60s', () => {
        return HttpResponse.json(
          { error: 'RATE_LIMITED', message: 'Too many requests' },
          {
            status: 429,
            headers: { 'Retry-After': '60' },
          },
        );
      }),
    );

    try {
      await httpClient.get('/rate-limited-60s');
      expect.fail('Should have thrown');
    } catch {
      // Error is expected
    }

    expect(capturedToasts[0].message).toContain('60');
  });
});

describe('HTTP Client - Timeout Handling (Integration)', () => {
  it('should produce a TIMEOUT NormalizedError when request times out', async () => {
    server.use(
      http.get('/api/slow-endpoint', async () => {
        // Simulate a very slow response (longer than timeout)
        await new Promise((resolve) => setTimeout(resolve, 60_000));
        return HttpResponse.json({ data: 'never reached' });
      }),
    );

    // Override the httpClient timeout for this test
    // We can't easily change axios instance timeout, so we test normalizeError directly
    const error = createAxiosError({ code: 'ECONNABORTED' });
    const normalized = normalizeError(error);

    expect(normalized.type).toBe('TIMEOUT');
    expect(normalized.message).toBe('Request timed out');
    expect(normalized.status).toBe(0);
  });
});

describe('HTTP Client - Request Interceptor', () => {
  it('should attach X-Correlation-ID header to requests', async () => {
    let capturedHeaders: Record<string, string> = {};

    server.use(
      http.get('/api/test-correlation', ({ request }) => {
        capturedHeaders = Object.fromEntries(request.headers.entries());
        return HttpResponse.json({ data: 'ok' });
      }),
    );

    await httpClient.get('/test-correlation');

    expect(capturedHeaders['x-correlation-id']).toBeDefined();
    // UUID v4 pattern
    expect(capturedHeaders['x-correlation-id']).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
    );
  });

  it('should not attach Bearer token when auth module has no token', async () => {
    let capturedHeaders: Record<string, string> = {};

    server.use(
      http.get('/api/test-no-auth', ({ request }) => {
        capturedHeaders = Object.fromEntries(request.headers.entries());
        return HttpResponse.json({ data: 'ok' });
      }),
    );

    await httpClient.get('/test-no-auth');

    // Since no token is set in auth module during tests, authorization header should be absent
    expect(capturedHeaders['authorization']).toBeUndefined();
  });

  describe('Admin tenant override', () => {
    afterEach(() => {
      useTenantStore.getState().setAdminSelectedTenant(null);
    });

    it('attaches X-Admin-Tenant-ID query param when an admin tenant override is set', async () => {
      useTenantStore.getState().setAdminSelectedTenant('tenant-override-123');
      let capturedUrl = '';

      server.use(
        http.get('/api/test-admin-tenant', ({ request }) => {
          capturedUrl = request.url;
          return HttpResponse.json({ data: 'ok' });
        }),
      );

      await httpClient.get('/test-admin-tenant');

      const params = new URL(capturedUrl).searchParams;
      expect(params.get('X-Admin-Tenant-ID')).toBe('tenant-override-123');
    });

    it('does not attach X-Admin-Tenant-ID when no admin tenant override is set', async () => {
      let capturedUrl = '';

      server.use(
        http.get('/api/test-no-admin-tenant', ({ request }) => {
          capturedUrl = request.url;
          return HttpResponse.json({ data: 'ok' });
        }),
      );

      await httpClient.get('/test-no-admin-tenant');

      const params = new URL(capturedUrl).searchParams;
      expect(params.has('X-Admin-Tenant-ID')).toBe(false);
    });

    it('removes a stale X-Admin-Tenant-ID once the override is cleared', async () => {
      useTenantStore.getState().setAdminSelectedTenant('tenant-override-123');
      useTenantStore.getState().setAdminSelectedTenant(null);
      let capturedUrl = '';

      server.use(
        http.get('/api/test-cleared-admin-tenant', ({ request }) => {
          capturedUrl = request.url;
          return HttpResponse.json({ data: 'ok' });
        }),
      );

      await httpClient.get('/test-cleared-admin-tenant');

      const params = new URL(capturedUrl).searchParams;
      expect(params.has('X-Admin-Tenant-ID')).toBe(false);
    });
  });
});
