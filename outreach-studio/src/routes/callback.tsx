/**
 * OAuth Callback Route
 *
 * Handles the redirect back from Keycloak after authentication.
 * Extracts `code` and `state` from URL search params and exchanges
 * them for tokens via authModule.handleCallback().
 */

import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useEffect, useState } from 'react';
import { authModule } from '@/lib/auth';

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
        // Redirect to dashboard after successful auth
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
      <div
        role="alert"
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
        <h1>Authentication Failed</h1>
        <p>{error}</p>
        <a href="/login">Return to login</a>
      </div>
    );
  }

  return (
    <div
      role="status"
      aria-label="Processing authentication"
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        width: '100%',
      }}
    >
      <span>Processing authentication...</span>
    </div>
  );
}
