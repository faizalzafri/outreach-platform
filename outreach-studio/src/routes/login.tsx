/**
 * Login Route
 *
 * Branded login page for Outreach Studio. Shows a centered card with
 * the application branding and a "Sign In" button that initiates the
 * OAuth2 Authorization Code + PKCE flow via authModule.login().
 *
 * If the user is already authenticated, redirects to the dashboard
 * (or to the URL specified in the `redirect` search param).
 */

import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { authModule } from '@/lib/auth';
import styles from './login.module.css';

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
  const [isRedirecting, setIsRedirecting] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      void navigate({ to: redirectUrl || '/' });
    }
  }, [isAuthenticated, navigate, redirectUrl]);

  const handleSignIn = useCallback(() => {
    setIsRedirecting(true);
    authModule.login();
  }, []);

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        {/* Brand */}
        <div className={styles.brand}>
          <span className={styles.brandIcon} aria-hidden="true">
            ◈
          </span>
          <span className={styles.brandName}>Outreach Studio</span>
        </div>

        {/* Heading */}
        <h1 className={styles.heading}>Welcome back</h1>
        <p className={styles.subtitle}>
          Sign in to continue to Outreach Studio
        </p>

        {/* Sign In Button / Redirecting State */}
        {isRedirecting ? (
          <div className={styles.redirecting} role="status" aria-label="Redirecting to authentication">
            <div className={styles.spinner} />
            <span className={styles.redirectingText}>
              Redirecting to authentication…
            </span>
          </div>
        ) : (
          <button
            type="button"
            className={styles.button}
            onClick={handleSignIn}
            disabled={isAuthenticated}
          >
            Sign In
          </button>
        )}

        {/* Footer */}
        <p className={styles.footer}>
          Secured with OAuth 2.0 + PKCE
        </p>
      </div>
    </div>
  );
}
