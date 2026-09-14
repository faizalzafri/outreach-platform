/**
 * Authenticated Layout Route
 *
 * Guards all child routes behind authentication.
 * Redirects unauthenticated users to /login, preserving the requested URL
 * as a `redirect` search param so the user can be returned after login.
 */

import { createFileRoute, Outlet, useNavigate } from '@tanstack/react-router';
import { useAuth } from '@/hooks/useAuth';
import { LayoutShell } from '@/components/layout';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary';
import { useEffect } from 'react';

export const Route = createFileRoute('/_authenticated')({
  component: AuthenticatedLayout,
  pendingComponent: PageSkeleton,
});

function AuthenticatedLayout() {
  const { isAuthenticated, isLoading, tenantSelectionRequired } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      // Preserve the current URL so the user can be redirected back after login
      const currentPath = window.location.pathname + window.location.search;
      void navigate({
        to: '/login',
        search: { redirect: currentPath },
      });
    }
  }, [isLoading, isAuthenticated, navigate]);

  useEffect(() => {
    // Checked on every protected-route render, not just right after the OAuth callback — a user
    // who never completed tenant selection (closed the tab, navigated away) would otherwise keep
    // operating under an arbitrarily-picked tenant indefinitely.
    if (!isLoading && isAuthenticated && tenantSelectionRequired) {
      void navigate({ to: '/select-tenant' });
    }
  }, [isLoading, isAuthenticated, tenantSelectionRequired, navigate]);

  if (isLoading) {
    return (
      <div
        role="status"
        aria-label="Loading"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100vh',
          width: '100%',
        }}
      >
        <span>Loading...</span>
      </div>
    );
  }

  if (!isAuthenticated || tenantSelectionRequired) {
    return null;
  }

  return (
    <LayoutShell>
      <ErrorBoundary>
        <Outlet />
      </ErrorBoundary>
    </LayoutShell>
  );
}
