/**
 * Dashboard Route
 *
 * Main landing page for authenticated users showing KPIs and charts.
 */

import { createFileRoute } from '@tanstack/react-router';
import { DashboardContent } from './-components/DashboardContent';

export const Route = createFileRoute('/_authenticated/dashboard')({
  component: DashboardPage,
});

function DashboardPage() {
  return (
    <DashboardContent />
  );
}
