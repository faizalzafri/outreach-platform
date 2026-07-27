/**
 * OAuth Callback Route
 *
 * Handles the redirect back from the authorization server after authentication.
 * Extracts `code` and `state` from URL search params and exchanges
 * them for tokens via authModule.handleCallback().
 *
 * Shows a branded "Verifying your identity" state with a spinner,
 * and a styled error state if something goes wrong.
 */

import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useEffect, useState } from 'react';
import { authModule } from '@/lib/auth';
import styles from './login.module.css';

interface CallbackSearchParams {
  code?: string;
  state?: string;
}

export const Route = createFileRoute('/callback')({
  validateSearch: (search: Record<string, unknown>): CallbackSearchParams => ({
    code: typeof search.code === 'string' ? search.code : undefined,
    state: typeof search.state === 'string' ? search.state : undefined,
  }),
  component: CallbackPage,
});

function CallbackPage() {
  const { code, state } = Route.useSearch();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function handleOAuthCallback() {
      if (!code || !state) {
        setError('Missing authorization code or state parameter.');
        return;
      }

      try {
        await authModule.handleCallback(code, state);
        void navigate({ to: '/' });
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Authentication failed';
        setError(message);
      }
    }

    void handleOAuthCallback();
  }, [code, state, navigate]);

  if (error) {
    return (
      <div className={styles.page}>
        <div className={styles.card} role="alert">
          <div className={styles.brand}>
            <span className={styles.brandIcon} aria-hidden="true">
              ◈
            </span>
            <span className={styles.brandName}>Outreach Studio</span>
          </div>

          <h1 className={styles.heading}>Authentication Failed</h1>
          <p className={styles.subtitle}>{error}</p>

          <a href="/login" className={styles.button} style={{ textDecoration: 'none' }}>
            Return to Sign In
          </a>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.brand}>
          <span className={styles.brandIcon} aria-hidden="true">
            ◈
          </span>
          <span className={styles.brandName}>Outreach Studio</span>
        </div>

        <div
          className={styles.redirecting}
          role="status"
          aria-label="Verifying your identity"
        >
          <div className={styles.spinner} />
          <span className={styles.redirectingText}>
            Verifying your identity…
          </span>
        </div>
      </div>
    </div>
  );
}
