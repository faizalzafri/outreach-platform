/**
 * Root Layout Route
 *
 * Wraps the entire application with:
 * - QueryClientProvider (TanStack Query)
 * - AuthProvider (authentication context)
 * - ToastContainer (global notifications)
 * - TanStack Router Devtools (development only)
 *
 * Requirements: 3.1
 */

import { createRootRoute, Outlet } from '@tanstack/react-router';
import { QueryClientProvider } from '@tanstack/react-query';
import { lazy, Suspense } from 'react';
import { queryClient } from '@/lib/query-client';
import { AuthProvider } from '@/hooks/useAuth';
import { ToastContainer } from '@/components/feedback/ToastContainer';

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
      <a href="/" style={{ color: '#3b82f6', textDecoration: 'underline' }}>
        Back to Dashboard
      </a>
    </div>
  );
}

function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <Outlet />
        <ToastContainer />
        <Suspense fallback={null}>
          <TanStackRouterDevtools position="bottom-right" />
        </Suspense>
      </AuthProvider>
    </QueryClientProvider>
  );
}
