/**
 * Authenticated Layout Route
 *
 * Guards all child routes behind authentication.
 * Redirects unauthenticated users to /login, preserving the requested URL
 * as a `redirect` search param so the user can be returned after login.
 *
 * Requirements: 3.6, 3.7
 */

import { createFileRoute, Outlet, redirect } from '@tanstack/react-router';
import { useAuth } from '@/hooks/useAuth';

export const Route = createFileRoute('/_authenticated')({
  component: AuthenticatedLayout,
});

function AuthenticatedLayout() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    // Preserve the current URL so the user can be redirected back after login
    const currentPath = window.location.pathname + window.location.search;
    const loginUrl = `/login?redirect=${encodeURIComponent(currentPath)}`;
    window.location.href = loginUrl;
    return null;
  }

  return <Outlet />;
}
