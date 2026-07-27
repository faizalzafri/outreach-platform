/**
 * usePermission Hook
 *
 * Checks whether the current authenticated user has at least one of the
 * required roles. Returns a boolean indicating access.
 *
 * Multi-role users are granted the union of all their role permissions —
 * if ANY of the user's roles matches ANY of the required roles, access is granted.
 */

import { useAuth } from '@/hooks/useAuth';

export interface UsePermissionResult {
  hasPermission: boolean;
  isLoading: boolean;
}

/**
 * Returns true if the current user holds at least one of the specified roles.
 * If requiredRoles is empty, access is always granted (public route).
 * While auth state is loading, returns false for permission and true for isLoading.
 */
export function usePermission(requiredRoles: string[]): UsePermissionResult {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return { hasPermission: false, isLoading: true };
  }

  // If no roles required, the route is accessible to any authenticated user
  if (requiredRoles.length === 0) {
    return { hasPermission: true, isLoading: false };
  }

  // If user is not available (not authenticated), deny access
  if (!user || !user.roles || user.roles.length === 0) {
    return { hasPermission: false, isLoading: false };
  }

  // Multi-role: grant access if user has AT LEAST ONE of the required roles
  const hasPermission = requiredRoles.some((role) => user.roles.includes(role));

  return { hasPermission, isLoading: false };
}
