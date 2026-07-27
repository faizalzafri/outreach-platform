/**
 * 404 Not Found Page
 *
 * Displayed when no route matches the current URL.
 * Provides a link back to the dashboard.
 *
 * Requirements: 3.7
 */

import { createFileRoute, Link } from '@tanstack/react-router';

export const Route = createFileRoute('/404')({
  component: NotFoundPage,
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
      <p>The page you're looking for doesn't exist.</p>
      <Link to="/" style={{ color: '#3b82f6', textDecoration: 'underline' }}>
        Back to Dashboard
      </Link>
    </div>
  );
}
