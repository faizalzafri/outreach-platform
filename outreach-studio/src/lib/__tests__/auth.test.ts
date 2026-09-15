import { describe, it, expect } from 'vitest';
import {
  generateCodeVerifier,
  generateCodeChallenge,
  generateState,
  decodeJwtPayload,
  extractRoles,
  isTokenExpiringSoon,
  hasValidTenantClaims,
} from '@/lib/auth';

// Helper: create a minimal JWT token with given claims
function createMockJwt(claims: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'RS256', typ: 'JWT' }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  const payload = btoa(JSON.stringify(claims))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `${header}.${payload}.mock-signature`;
}

describe('Auth Module - PKCE Generation', () => {
  describe('generateCodeVerifier', () => {
    it('should produce a string between 43 and 128 characters', () => {
      const verifier = generateCodeVerifier();
      expect(verifier.length).toBeGreaterThanOrEqual(43);
      expect(verifier.length).toBeLessThanOrEqual(128);
    });

    it('should contain only base64url-safe characters', () => {
      const verifier = generateCodeVerifier();
      // base64url characters: A-Z, a-z, 0-9, -, _
      expect(verifier).toMatch(/^[A-Za-z0-9\-_]+$/);
    });

    it('should produce unique values on each call', () => {
      const verifiers = new Set(Array.from({ length: 10 }, () => generateCodeVerifier()));
      expect(verifiers.size).toBe(10);
    });
  });

  describe('generateCodeChallenge', () => {
    it('should produce a base64url-encoded SHA-256 hash of the verifier', async () => {
      const verifier = generateCodeVerifier();
      const challenge = await generateCodeChallenge(verifier);

      // S256 challenge should be 43 chars (256 bits base64url-encoded)
      expect(challenge.length).toBe(43);
      expect(challenge).toMatch(/^[A-Za-z0-9\-_]+$/);
    });

    it('should produce the same challenge for the same verifier', async () => {
      const verifier = 'test-verifier-constant-value-1234567890abc';
      const challenge1 = await generateCodeChallenge(verifier);
      const challenge2 = await generateCodeChallenge(verifier);
      expect(challenge1).toBe(challenge2);
    });

    it('should produce different challenges for different verifiers', async () => {
      const challenge1 = await generateCodeChallenge('verifier-one-abcdefghijklmnopqrstuvwxy');
      const challenge2 = await generateCodeChallenge('verifier-two-abcdefghijklmnopqrstuvwxy');
      expect(challenge1).not.toBe(challenge2);
    });
  });

  describe('generateState', () => {
    it('should produce a valid UUID', () => {
      const state = generateState();
      const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
      expect(state).toMatch(uuidRegex);
    });

    it('should produce unique values on each call', () => {
      const states = new Set(Array.from({ length: 10 }, () => generateState()));
      expect(states.size).toBe(10);
    });
  });
});

describe('Auth Module - Token Decoding and Role Extraction', () => {
  describe('decodeJwtPayload', () => {
    it('should decode a valid JWT payload', () => {
      const claims = {
        sub: 'user-001',
        name: 'John Doe',
        email: 'john@example.com',
        realm_access: { roles: ['ROLE_ADMIN'] },
        exp: 9999999999,
        iat: 1717000000,
      };
      const token = createMockJwt(claims);
      const decoded = decodeJwtPayload(token);

      expect(decoded.sub).toBe('user-001');
      expect(decoded.name).toBe('John Doe');
      expect(decoded.email).toBe('john@example.com');
      expect(decoded.exp).toBe(9999999999);
    });

    it('should throw for a token with missing payload segment', () => {
      expect(() => decodeJwtPayload('header-only')).toThrow('Invalid JWT: missing payload segment');
    });

    it('should handle tokens without optional claims', () => {
      const claims = { sub: 'user-002', exp: 9999999999, iat: 1717000000 };
      const token = createMockJwt(claims);
      const decoded = decodeJwtPayload(token);

      expect(decoded.sub).toBe('user-002');
      expect(decoded.name).toBeUndefined();
      expect(decoded.email).toBeUndefined();
    });
  });

  describe('extractRoles', () => {
    it('should extract roles from realm_access.roles', () => {
      const claims = {
        sub: 'user-001',
        realm_access: { roles: ['ROLE_ADMIN', 'ROLE_PMO'] },
        exp: 9999999999,
        iat: 1717000000,
      };
      const roles = extractRoles(claims);
      expect(roles).toEqual(['ROLE_ADMIN', 'ROLE_PMO']);
    });

    it('should return empty array when realm_access is missing', () => {
      const claims = { sub: 'user-001', exp: 9999999999, iat: 1717000000 };
      const roles = extractRoles(claims);
      expect(roles).toEqual([]);
    });

    it('should return empty array when roles array is missing', () => {
      const claims = { sub: 'user-001', realm_access: {}, exp: 9999999999, iat: 1717000000 };
      const roles = extractRoles(claims);
      expect(roles).toEqual([]);
    });

    it('should handle a single role', () => {
      const claims = {
        sub: 'user-001',
        realm_access: { roles: ['ROLE_POC'] },
        exp: 9999999999,
        iat: 1717000000,
      };
      const roles = extractRoles(claims);
      expect(roles).toEqual(['ROLE_POC']);
    });
  });

  describe('isTokenExpiringSoon', () => {
    it('should return true when token expires within the threshold', () => {
      const nowSeconds = Math.floor(Date.now() / 1000);
      const token = createMockJwt({
        sub: 'user-001',
        exp: nowSeconds + 30, // expires in 30 seconds
        iat: nowSeconds - 270,
      });
      expect(isTokenExpiringSoon(token, 60)).toBe(true);
    });

    it('should return false when token has plenty of time remaining', () => {
      const nowSeconds = Math.floor(Date.now() / 1000);
      const token = createMockJwt({
        sub: 'user-001',
        exp: nowSeconds + 300, // expires in 5 minutes
        iat: nowSeconds - 60,
      });
      expect(isTokenExpiringSoon(token, 60)).toBe(false);
    });

    it('should return true when token is already expired', () => {
      const nowSeconds = Math.floor(Date.now() / 1000);
      const token = createMockJwt({
        sub: 'user-001',
        exp: nowSeconds - 10, // expired 10 seconds ago
        iat: nowSeconds - 310,
      });
      expect(isTokenExpiringSoon(token, 60)).toBe(true);
    });
  });

  describe('hasValidTenantClaims', () => {
    const base = { sub: 'user-001', exp: 9999999999, iat: 1717000000 };

    it('is valid for a regular user with a tenant_id', () => {
      expect(hasValidTenantClaims({ ...base, tenant_id: 'tenant-123' })).toBe(true);
    });

    it('is valid for a Platform Admin with no tenant_id', () => {
      expect(hasValidTenantClaims({ ...base, platform_admin: true })).toBe(true);
    });

    it('is invalid when neither tenant_id nor platform_admin is present', () => {
      expect(hasValidTenantClaims({ ...base })).toBe(false);
    });

    it('is invalid when platform_admin is explicitly false and tenant_id is missing', () => {
      expect(hasValidTenantClaims({ ...base, platform_admin: false })).toBe(false);
    });
  });
});
