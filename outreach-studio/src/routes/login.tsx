/**
 * Login Route
 *
 * Initiates the OAuth2 Authorization Code + PKCE flow by redirecting
 * the user to Keycloak. If the user is already authenticated, redirects
 * to the dashboard (or to the URL specified in the `redirect` search param).
 *
 * Requirements: 3.6
 */

import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useEffect } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { authModule } from '@/lib/auth';

interface LoginSearchParams {
  redirect?: string;
}

export const Route = createFileRoute('/login')({
  validateSearch: (search: Record<string, unknown>): LoginSearchParams => ({
    redirect: typeof search.redirect === 'string' ? search.redirect : undefined,
  }),
  component: LoginPage,
});

function LoginPage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const { redirect: redirectUrl } = Route.useSearch();

  useEffect(() => {
    if (isAuthenticated) {
      // Already authenticated — redirect to requested URL or dashboard
      void navigate({ to: redirectUrl || '/' });
      return;
    }

    // Initiate OAuth login flow
    authModule.login();
  }, [isAuthenticated, navigate, redirectUrl]);

  return (
    <div
      role="status"
      aria-label="Redirecting to login"
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        width: '100%',
      }}
    >
      <span>Redirecting to login...</span>
    </div>
  );
}
