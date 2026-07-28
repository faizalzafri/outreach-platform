import { describe, it, expect, vi } from 'vitest';
import { renderHook } from '@testing-library/react';
import { usePermission } from '../usePermission';

// Mock the useAuth hook to control user state
vi.mock('@/hooks/useAuth', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from '@/hooks/useAuth';
const mockUseAuth = vi.mocked(useAuth);

describe('usePermission', () => {
  it('returns hasPermission=true when user has a matching role', () => {
    mockUseAuth.mockReturnValue({
      user: { sub: '1', name: 'Admin', email: 'a@b.c', roles: ['ROLE_ADMIN'] },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    const { result } = renderHook(() => usePermission(['ROLE_ADMIN']));

    expect(result.current.hasPermission).toBe(true);
    expect(result.current.isLoading).toBe(false);
  });

  it('returns hasPermission=false when user lacks all required roles', () => {
    mockUseAuth.mockReturnValue({
      user: { sub: '1', name: 'POC User', email: 'poc@b.c', roles: ['ROLE_POC'] },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    const { result } = renderHook(() => usePermission(['ROLE_ADMIN']));

    expect(result.current.hasPermission).toBe(false);
    expect(result.current.isLoading).toBe(false);
  });

  it('grants access when user has at least one of multiple required roles', () => {
    mockUseAuth.mockReturnValue({
      user: { sub: '1', name: 'PMO User', email: 'pmo@b.c', roles: ['ROLE_PMO'] },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    const { result } = renderHook(() => usePermission(['ROLE_ADMIN', 'ROLE_PMO']));

    expect(result.current.hasPermission).toBe(true);
  });

  it('grants access for multi-role users (union of permissions)', () => {
    mockUseAuth.mockReturnValue({
      user: { sub: '1', name: 'Multi', email: 'm@b.c', roles: ['ROLE_PMO', 'ROLE_POC'] },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    const { result } = renderHook(() => usePermission(['ROLE_POC']));

    expect(result.current.hasPermission).toBe(true);
  });

  it('returns hasPermission=true when requiredRoles is empty (public route)', () => {
    mockUseAuth.mockReturnValue({
      user: { sub: '1', name: 'User', email: 'u@b.c', roles: ['ROLE_POC'] },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    const { result } = renderHook(() => usePermission([]));

    expect(result.current.hasPermission).toBe(true);
  });

  it('returns hasPermission=false and isLoading=true while auth is loading', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: true,
      login: vi.fn(),
      logout: vi.fn(),
    });

    const { result } = renderHook(() => usePermission(['ROLE_ADMIN']));

    expect(result.current.hasPermission).toBe(false);
    expect(result.current.isLoading).toBe(true);
  });

  it('returns hasPermission=false when user is null (unauthenticated)', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    const { result } = renderHook(() => usePermission(['ROLE_ADMIN']));

    expect(result.current.hasPermission).toBe(false);
    expect(result.current.isLoading).toBe(false);
  });

  it('returns hasPermission=false when user has empty roles array', () => {
    mockUseAuth.mockReturnValue({
      user: { sub: '1', name: 'No Roles', email: 'n@b.c', roles: [] },
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    const { result } = renderHook(() => usePermission(['ROLE_ADMIN']));

    expect(result.current.hasPermission).toBe(false);
  });
});
