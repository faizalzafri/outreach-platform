/**
 * Dashboard Route
 *
 * Main landing page for authenticated users showing KPIs and charts.
 * Uses lazy loading for code splitting.
 *
 * Requirements: 3.2, 3.3
 */

import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/dashboard')({
  component: DashboardPage,
});

function DashboardPage() {
  return (
    <div>
      <h1>Dashboard</h1>
      <p>Welcome to the Outreach FMS Dashboard.</p>
    </div>
  );
}
