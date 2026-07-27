/**
 * Authenticated Index Route
 *
 * Landing page for authenticated users (dashboard placeholder).
 * This is a temporary placeholder until the dashboard is implemented in task 11.
 */

import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/')({
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
      <p style={{ marginLeft: '1rem' }}>Welcome to the Dashboard</p>
    </div>
  );
}
