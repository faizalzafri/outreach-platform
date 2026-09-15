/**
 * HTTP Client with Axios interceptors for the Outreach FMS SPA.
 *
 * Features:
 * - Base URL `/api` with 30s timeout
 * - Request interceptor: attaches Bearer token (skips Keycloak endpoints),
 *   adds X-Correlation-ID (UUID v4)
 * - Response interceptor: normalizes all errors into NormalizedError
 * - 401 handling: single silent refresh attempt + retry, then redirect to login
 * - 429 handling: warning toast with Retry-After duration
 * - Timeout handling: NormalizedError with type TIMEOUT
 * - Non-standard error bodies: UNKNOWN_ERROR type with statusText
 */

import axios from 'axios';
import type { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { v4 as uuidv4 } from 'uuid';

import { authModule } from '@/lib/auth';
import { useTenantStore } from '@/stores/tenant-store';
import type { NormalizedError, ToastPayload } from '@/types/api';

// --- Keycloak endpoint detection ---

const KEYCLOAK_PATH_SEGMENTS = ['/realms/', '/protocol/openid-connect/'] as const;

/**
 * Returns true if the URL is a Keycloak identity provider endpoint
 * (should not have Bearer token attached).
 */
function isKeycloakEndpoint(url: string | undefined): boolean {
  if (!url) return false;
  return KEYCLOAK_PATH_SEGMENTS.some((segment) => url.includes(segment));
}

// --- Toast callback ---

/**
 * Pluggable toast display function. Defaults to a no-op until the toast system
 * is wired up. Call `setToastHandler` to connect the real implementation.
 */
let toastHandler: (toast: ToastPayload) => void = () => {
  // no-op placeholder — replaced once the toast system is initialized
};

/**
 * Registers the real toast handler. Called by the toast system during app bootstrap.
 */
export function setToastHandler(handler: (toast: ToastPayload) => void): void {
  toastHandler = handler;
}

// --- Error normalization ---

/**
 * Transforms any AxiosError into a consistent NormalizedError structure.
 */
export function normalizeError(error: AxiosError): NormalizedError {
  const response = error.response;

  // No response — network error or timeout
  if (!response) {
    const isTimeout = error.code === 'ECONNABORTED';
    return {
      status: 0,
      type: isTimeout ? 'TIMEOUT' : 'NETWORK_ERROR',
      message: isTimeout ? 'Request timed out' : 'Network error',
      correlationId: null,
      fieldErrors: [],
    };
  }

  // Try to parse a standard backend error body
  const data = response.data as Record<string, unknown> | undefined;
  if (data && typeof data.error === 'string' && typeof data.message === 'string') {
    return {
      status: response.status,
      type: data.error,
      message: data.message,
      correlationId:
        (typeof data.correlationId === 'string' ? data.correlationId : null) ??
        (response.headers['x-correlation-id'] as string | undefined) ??
        null,
      fieldErrors: Array.isArray(data.fieldErrors)
        ? (data.fieldErrors as Array<{ field: string; message: string }>)
        : [],
    };
  }

  // Non-standard error body — produce UNKNOWN_ERROR
  return {
    status: response.status,
    type: 'UNKNOWN_ERROR',
    message: response.statusText || 'An unexpected error occurred',
    correlationId:
      (response.headers['x-correlation-id'] as string | undefined) ?? null,
    fieldErrors: [],
  };
}

// --- 401 handling with single silent refresh + retry ---

/** Tracks whether a token refresh is already in progress to avoid concurrent refreshes. */
let isRefreshing = false;

/**
 * Handles 401 responses by attempting a single silent token refresh.
 * If refresh succeeds, retries the original request with the new token.
 * If refresh fails, redirects to login.
 */
async function handleUnauthorized(
  error: AxiosError,
  instance: AxiosInstance,
): Promise<unknown> {
  const originalRequest = error.config as
    | (InternalAxiosRequestConfig & { _retried?: boolean })
    | undefined;

  // Only attempt one retry per request
  if (!originalRequest || originalRequest._retried) {
    authModule.login();
    throw normalizeError(error);
  }

  if (isRefreshing) {
    // Another refresh is already in progress — don't queue, just fail
    throw normalizeError(error);
  }

  originalRequest._retried = true;
  isRefreshing = true;

  try {
    const newToken = await authModule.silentRefresh();

    if (!newToken) {
      // Refresh failed — redirect to login
      authModule.login();
      throw normalizeError(error);
    }

    // Retry the original request with the refreshed token
    originalRequest.headers.Authorization = `Bearer ${newToken}`;
    return instance(originalRequest);
  } finally {
    isRefreshing = false;
  }
}

// --- 429 handling ---

/**
 * Handles 429 Too Many Requests by displaying a warning toast
 * with the Retry-After duration.
 */
function handleRateLimited(error: AxiosError): void {
  const retryAfter = error.response?.headers['retry-after'] as string | undefined;
  const seconds = retryAfter ? parseInt(retryAfter, 10) : undefined;

  const message =
    seconds && !Number.isNaN(seconds)
      ? `Rate limited. Please retry after ${seconds} seconds.`
      : 'Rate limited. Please try again later.';

  toastHandler({
    severity: 'warning',
    message,
  });
}

// --- Create the Axios instance ---

const httpClient: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30_000,
});

// --- Request Interceptor ---

httpClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // Attach Bearer token (skip Keycloak endpoints)
  const token = authModule.getAccessToken();
  if (token && !isKeycloakEndpoint(config.url)) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // Attach correlation ID for request tracing
  config.headers['X-Correlation-ID'] = uuidv4();

  // Platform Admin tenant override: when set, every outbound request carries the admin's chosen
  // tenant as a query param so the gateway can scope the request to it instead of (or in addition
  // to) the admin's own JWT tenant_id. Explicitly removed when not set, rather than left stale
  // from a previous request's config object.
  const { adminSelectedTenantId } = useTenantStore.getState();
  const params = { ...config.params };
  if (adminSelectedTenantId) {
    params['X-Admin-Tenant-ID'] = adminSelectedTenantId;
  } else {
    delete params['X-Admin-Tenant-ID'];
  }
  config.params = params;

  return config;
});

// --- Response Interceptor ---

httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    // 401: attempt silent refresh + retry
    if (error.response?.status === 401) {
      return handleUnauthorized(error, httpClient);
    }

    // 429: display rate-limit warning toast
    if (error.response?.status === 429) {
      handleRateLimited(error);
    }

    // All errors are normalized before propagation
    throw normalizeError(error);
  },
);

export { httpClient };
export type { NormalizedError } from '@/types/api';
