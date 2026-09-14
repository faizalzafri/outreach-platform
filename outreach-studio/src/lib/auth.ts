/**
 * OAuth2 Authorization Code + PKCE Authentication Module
 *
 * Implements the full Keycloak OIDC flow:
 * - PKCE (S256) challenge generation
 * - Login redirect with state parameter
 * - Callback handling with token exchange
 * - Silent refresh via hidden iframe
 * - Logout with token revocation
 * - JWT decoding for user profile extraction
 * - Auto-refresh when token lifetime < 60s
 *
 * Access tokens are stored in memory only (never localStorage).
 */

import type {
  AuthState,
  AuthModule,
  JwtClaims,
  KeycloakConfig,
  TokenResponse,
  UserProfile,
} from '@/types/auth';
import { useTenantStore } from '@/stores/tenant-store';

// --- Configuration ---

function getKeycloakConfig(): KeycloakConfig {
  return {
    url: import.meta.env.VITE_KEYCLOAK_URL as string || 'http://localhost:8090',
    realm: import.meta.env.VITE_KEYCLOAK_REALM as string || '',
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID as string || 'outreach-dashboard',
  };
}

function getKeycloakEndpoints(config: KeycloakConfig) {
  // If a realm is configured, use Keycloak-style endpoints
  if (config.realm) {
    const base = `${config.url}/realms/${config.realm}/protocol/openid-connect`;
    return {
      authorization: `${base}/auth`,
      token: `${base}/token`,
      logout: `${base}/logout`,
      revoke: `${base}/revoke`,
    };
  }

  // Otherwise, use Spring Authorization Server endpoints
  // Authorization endpoint uses the full URL (browser redirect)
  // Token/revoke use relative paths (proxied via Vite in dev, Nginx in prod)
  // Logout uses the full URL (browser redirect to end server session) — auth-service's own
  // SecurityConfig maps its logout endpoint at the plain Spring Security default path, /logout,
  // not the OIDC RP-initiated-logout path /connect/logout (which this auth server doesn't expose).
  return {
    authorization: `${config.url}/oauth2/authorize`,
    token: `/oauth2/token`,
    logout: `${config.url}/logout`,
    revoke: `/oauth2/revoke`,
  };
}

// --- PKCE Helpers ---

/**
 * Generates a cryptographically random code verifier (43-128 characters).
 * Uses base64url-safe characters: [A-Z, a-z, 0-9, -, ., _, ~]
 */
export function generateCodeVerifier(): string {
  const array = new Uint8Array(64);
  crypto.getRandomValues(array);
  return base64UrlEncode(array).slice(0, 128);
}

/**
 * Generates a SHA-256 code challenge from the verifier (S256 method).
 */
export async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return base64UrlEncode(new Uint8Array(digest));
}

/**
 * Generates a cryptographically random state parameter using crypto.randomUUID().
 */
export function generateState(): string {
  return crypto.randomUUID();
}

/**
 * Thrown by {@link createAuthModule}'s `handleCallback` when the auth server rejects token
 * issuance with the `NO_TENANT_ASSOCIATION` OAuth2 error — the authenticated user has no tenant
 * membership at all. The callback route checks for this specific type to redirect to `/no-tenant`
 * instead of showing a generic authentication-failed message.
 */
export class NoTenantAssociationError extends Error {
  constructor() {
    super('User has no tenant association');
    this.name = 'NoTenantAssociationError';
  }
}

// --- Token Helpers ---

/**
 * Decodes a JWT payload (without verifying signature — verification is server-side).
 */
export function decodeJwtPayload(token: string): JwtClaims {
  const parts = token.split('.');
  const payload = parts[1];
  if (!payload) {
    throw new Error('Invalid JWT: missing payload segment');
  }
  const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
  return JSON.parse(decoded) as JwtClaims;
}

/**
 * Extracts realm roles from JWT claims (from realm_access.roles).
 */
export function extractRoles(claims: JwtClaims): string[] {
  return claims.realm_access?.roles ?? [];
}

/**
 * A regular (non-Platform-Admin) user's token must always carry a tenant_id — the auth server
 * rejects token issuance server-side (NO_TENANT_ASSOCIATION) when a regular user has no active
 * tenant membership at all, so a token reaching the frontend without one, and without
 * platform_admin either, indicates a malformed or tampered token rather than a normal state to
 * recover from silently.
 */
export function hasValidTenantClaims(claims: JwtClaims): boolean {
  return Boolean(claims.tenant_id) || claims.platform_admin === true;
}

/**
 * Pushes tenant claims from a freshly-decoded token onto the Tenant Store. Called after both
 * initial token exchange and every silent refresh so the store never carries a stale tenant_id
 * across a token rotation.
 */
function syncTenantStore(claims: JwtClaims): void {
  useTenantStore.getState().setTenantFromJwt({
    tenant_id: claims.tenant_id,
    tenant_roles: claims.tenant_roles,
    platform_admin: claims.platform_admin,
  });
}

/**
 * Checks if a token's remaining lifetime is below the given threshold (in seconds).
 */
export function isTokenExpiringSoon(token: string, thresholdSeconds: number): boolean {
  const claims = decodeJwtPayload(token);
  const nowSeconds = Math.floor(Date.now() / 1000);
  const remainingSeconds = claims.exp - nowSeconds;
  return remainingSeconds < thresholdSeconds;
}

// --- Base64url encoding ---

function base64UrlEncode(bytes: Uint8Array): string {
  const binary = Array.from(bytes)
    .map((b) => String.fromCharCode(b))
    .join('');
  return btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

// --- Session storage for PKCE flow (temporary, cleared after callback) ---

const PKCE_VERIFIER_KEY = 'outreach_pkce_verifier';
const PKCE_STATE_KEY = 'outreach_pkce_state';

function storePkceParams(verifier: string, state: string): void {
  sessionStorage.setItem(PKCE_VERIFIER_KEY, verifier);
  sessionStorage.setItem(PKCE_STATE_KEY, state);
}

function retrievePkceParams(): { verifier: string | null; state: string | null } {
  return {
    verifier: sessionStorage.getItem(PKCE_VERIFIER_KEY),
    state: sessionStorage.getItem(PKCE_STATE_KEY),
  };
}

function clearPkceParams(): void {
  sessionStorage.removeItem(PKCE_VERIFIER_KEY);
  sessionStorage.removeItem(PKCE_STATE_KEY);
}

// --- Auth Module Factory ---

export function createAuthModule(): AuthModule {
  const config = getKeycloakConfig();
  const endpoints = getKeycloakEndpoints(config);

  // In-memory token storage (never in localStorage)
  let state: AuthState = {
    accessToken: null,
    refreshToken: null,
    user: null,
    isAuthenticated: false,
    isLoading: true,
    tenantSelectionRequired: false,
  };

  let refreshTimer: ReturnType<typeof setTimeout> | null = null;
  const listeners: Set<(state: AuthState) => void> = new Set();

  function setState(partial: Partial<AuthState>): void {
    state = { ...state, ...partial };
    listeners.forEach((listener) => listener(state));
  }

  /**
   * Decodes a freshly-issued access token once and derives everything a caller needs from it:
   * the user profile, whether tenant selection is still pending, and (as a side effect) syncing
   * the Tenant Store. Throws if the token is malformed or fails the tenant-claim sanity check —
   * callers must clear the session on that error, not retry.
   */
  function processNewToken(accessToken: string): { user: UserProfile; tenantSelectionRequired: boolean } {
    const claims = decodeJwtPayload(accessToken);

    if (!hasValidTenantClaims(claims)) {
      throw new Error('Token is missing tenant association: authentication failed');
    }

    syncTenantStore(claims);

    return {
      user: {
        sub: claims.sub,
        name: claims.name ?? claims.preferred_username ?? 'Unknown',
        email: claims.email ?? '',
        roles: extractRoles(claims),
      },
      tenantSelectionRequired: claims.tenant_selection_required ?? false,
    };
  }

  function scheduleAutoRefresh(accessToken: string): void {
    if (refreshTimer) {
      clearTimeout(refreshTimer);
      refreshTimer = null;
    }

    const claims = decodeJwtPayload(accessToken);
    const nowSeconds = Math.floor(Date.now() / 1000);
    const expiresInSeconds = claims.exp - nowSeconds;

    // Refresh when lifetime falls below 60 seconds
    const refreshInMs = Math.max((expiresInSeconds - 60) * 1000, 0);

    refreshTimer = setTimeout(() => {
      void silentRefresh();
    }, refreshInMs);
  }

  function getRedirectUri(): string {
    return `${window.location.origin}/callback`;
  }

  // --- Public API ---

  function login(): void {
    const verifier = generateCodeVerifier();
    const stateParam = generateState();

    storePkceParams(verifier, stateParam);

    // Generate challenge and redirect (async, but login() is fire-and-forget)
    void generateCodeChallenge(verifier).then((challenge) => {
      const params = new URLSearchParams({
        client_id: config.clientId,
        response_type: 'code',
        scope: 'openid profile email',
        redirect_uri: getRedirectUri(),
        state: stateParam,
        code_challenge: challenge,
        code_challenge_method: 'S256',
      });

      window.location.href = `${endpoints.authorization}?${params.toString()}`;
    });
  }

  async function handleCallback(code: string, callbackState: string): Promise<void> {
    const { verifier, state: storedState } = retrievePkceParams();
    clearPkceParams();

    // Validate state parameter
    if (!storedState || callbackState !== storedState) {
      clearSession();
      throw new Error('Invalid state parameter: authentication failed');
    }

    if (!verifier) {
      clearSession();
      throw new Error('Missing PKCE verifier: authentication failed');
    }

    // Exchange code for tokens (must complete within 10 seconds)
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10_000);

    try {
      const body = new URLSearchParams({
        grant_type: 'authorization_code',
        client_id: config.clientId,
        code,
        redirect_uri: getRedirectUri(),
        code_verifier: verifier,
      });

      const response = await fetch(endpoints.token, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body.toString(),
        signal: controller.signal,
      });

      if (!response.ok) {
        // The auth server rejects token issuance with this specific OAuth2 error code (RFC 6749
        // §5.2 error response) when an authenticated user has no tenant membership at all —
        // distinct from every other token-exchange failure, since the callback route needs to
        // send the user to a dedicated explanatory page rather than a generic error message.
        const errorBody = await response.json().catch(() => null) as { error?: string } | null;
        if (errorBody?.error === 'NO_TENANT_ASSOCIATION') {
          throw new NoTenantAssociationError();
        }
        throw new Error(`Token exchange failed: ${response.status} ${response.statusText}`);
      }

      const tokens = (await response.json()) as TokenResponse;
      const { user, tenantSelectionRequired } = processNewToken(tokens.access_token);

      setState({
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
        user,
        isAuthenticated: true,
        isLoading: false,
        tenantSelectionRequired,
      });

      scheduleAutoRefresh(tokens.access_token);
    } catch (error) {
      clearSession();

      if (error instanceof DOMException && error.name === 'AbortError') {
        throw new Error('Token exchange timed out: authentication could not be completed', { cause: error });
      }
      throw error;
    } finally {
      clearTimeout(timeout);
    }
  }

  async function silentRefresh(): Promise<string | null> {
    if (!state.refreshToken) {
      clearSession();
      return null;
    }

    try {
      const body = new URLSearchParams({
        grant_type: 'refresh_token',
        client_id: config.clientId,
        refresh_token: state.refreshToken,
      });

      const response = await fetch(endpoints.token, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body.toString(),
      });

      if (!response.ok) {
        // Refresh token expired or invalid — redirect to login
        clearSession();
        return null;
      }

      const tokens = (await response.json()) as TokenResponse;
      // Atomic shallow merge — updates token-derived fields only, no navigation or unmount, so
      // in-progress form state elsewhere in the app is untouched by a background token rotation.
      const { user, tenantSelectionRequired } = processNewToken(tokens.access_token);

      setState({
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
        user,
        isAuthenticated: true,
        isLoading: false,
        tenantSelectionRequired,
      });

      scheduleAutoRefresh(tokens.access_token);
      return tokens.access_token;
    } catch {
      // Network error, malformed token, or missing tenant claims — all treated the same way:
      // clear the session and let the caller redirect to login.
      clearSession();
      return null;
    }
  }

  async function logout(): Promise<void> {
    const currentRefreshToken = state.refreshToken;

    // Attempt to revoke tokens
    if (currentRefreshToken) {
      try {
        const body = new URLSearchParams({
          client_id: config.clientId,
          token: currentRefreshToken,
          token_type_hint: 'refresh_token',
        });

        await fetch(endpoints.revoke, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: body.toString(),
        });
      } catch {
        // Best-effort revocation — proceed with local cleanup regardless
      }
    }

    clearSession();

    // Redirect to auth server's logout which invalidates the session
    // and redirects back to our login page (configured in SecurityConfig)
    window.location.href = endpoints.logout;
  }

  function getAccessToken(): string | null {
    return state.accessToken;
  }

  function getUser(): UserProfile | null {
    return state.user;
  }

  function getState(): AuthState {
    return state;
  }

  function onStateChange(listener: (state: AuthState) => void): () => void {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  }

  async function initialize(): Promise<void> {
    // On app startup, try to detect if we're in a callback or have existing session
    // If there's no refresh token in memory, we cannot restore session
    // The app should redirect to login for unauthenticated users
    setState({ isLoading: false });
  }

  function clearSession(): void {
    if (refreshTimer) {
      clearTimeout(refreshTimer);
      refreshTimer = null;
    }

    setState({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      isLoading: false,
      tenantSelectionRequired: false,
    });

    useTenantStore.getState().clearTenant();
  }

  return {
    login,
    logout,
    handleCallback,
    getAccessToken,
    silentRefresh,
    getUser,
    getState,
    onStateChange,
    initialize,
  };
}

// Singleton auth module instance
export const authModule = createAuthModule();
