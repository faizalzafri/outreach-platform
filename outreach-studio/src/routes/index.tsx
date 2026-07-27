/**
 * Index Route
 *
 * Redirects to the authenticated dashboard.
 * This is a temporary placeholder until the dashboard is implemented.
 */

import { createFileRoute, redirect } from '@tanstack/react-router';

export const Route = createFileRoute('/')({
  beforeLoad: () => {
    // Once authenticated routes are set up, this can redirect to /dashboard
    // For now, render a simple landing
  },
  component: IndexPage,
});

function IndexPage() {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        width: '100%',
      }}
    >
      <h1>Outreach Studio</h1>
    </div>
  );
}
