/**
 * Root Layout Route
 *
 * Wraps the entire application with:
 * - QueryClientProvider (TanStack Query)
 * - AuthProvider (authentication context)
 * - ErrorBoundary (catches render errors in the tree)
 * - ConnectivityMonitor (offline/system unavailable banners)
 * - ToastContainer (global notifications)
 * - Route announcer (aria-live region for navigation)
 * - TanStack Router Devtools (development only)
 */

import { createRootRoute, Outlet, useRouterState } from '@tanstack/react-router';
import { QueryClientProvider } from '@tanstack/react-query';
import { lazy, Suspense, useEffect, useRef, useState } from 'react';
import { queryClient } from '@/lib/query-client';
import { AuthProvider } from '@/hooks/useAuth';
import { ToastContainer } from '@/components/feedback/ToastContainer';
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary';
import { ConnectivityMonitor } from '@/components/feedback/ConnectivityMonitor';

const TanStackRouterDevtools =
  import.meta.env.MODE === 'production'
    ? () => null
    : lazy(() =>
        import('@tanstack/router-devtools').then((mod) => ({
          default: mod.TanStackRouterDevtools,
        }))
      );

export const Route = createRootRoute({
  component: RootLayout,
  notFoundComponent: NotFoundPage,
});

function NotFoundPage() {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        width: '100%',
        gap: '1rem',
      }}
    >
      <h1>404 — Page Not Found</h1>
      <p>The page you&apos;re looking for doesn&apos;t exist.</p>
      <a href="/" style={{ color: 'var(--accent)', textDecoration: 'underline' }}>
        Back to Dashboard
      </a>
    </div>
  );
}

/**
 * Announces route changes to screen readers via an aria-live region.
 * Derives a human-readable page title from the current pathname.
 */
function RouteAnnouncer() {
  const routerState = useRouterState();
  const pathname = routerState.location.pathname;
  const [announcement, setAnnouncement] = useState('');
  const isFirstRender = useRef(true);

  useEffect(() => {
    // Skip the first render to avoid announcing the initial page load
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }

    const pageTitle = derivePageTitle(pathname);
    setAnnouncement(`Navigated to ${pageTitle}`);
  }, [pathname]);

  return (
    <div
      role="status"
      aria-live="polite"
      aria-atomic="true"
      className="sr-only"
    >
      {announcement}
    </div>
  );
}

/**
 * Derives a human-readable page title from a URL pathname.
 */
function derivePageTitle(pathname: string): string {
  const segments = pathname.split('/').filter(Boolean);

  if (segments.length === 0) return 'Dashboard';

  // Map known route segments to readable names
  const nameMap: Record<string, string> = {
    dashboard: 'Dashboard',
    events: 'Events',
    volunteers: 'Volunteers',
    feedback: 'Feedback',
    ingestion: 'Data Import',
    notifications: 'Notifications',
    reports: 'Reports',
    admin: 'Administration',
    'audit-log': 'Audit Log',
    create: 'Create',
    login: 'Login',
    callback: 'Authentication',
  };

  const lastSegment = segments[segments.length - 1]!;
  return nameMap[lastSegment] ?? lastSegment.replace(/-/g, ' ').replace(/^\w/, (c) => c.toUpperCase());
}

function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ErrorBoundary>
          <ConnectivityMonitor />
          <Outlet />
        </ErrorBoundary>
        <RouteAnnouncer />
        <ToastContainer />
        <Suspense fallback={null}>
          <TanStackRouterDevtools position="bottom-right" />
        </Suspense>
      </AuthProvider>
    </QueryClientProvider>
  );
}
