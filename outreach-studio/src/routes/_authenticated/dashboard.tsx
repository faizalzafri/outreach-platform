/**
 * Dashboard Route
 *
 * Main landing page for authenticated users showing KPIs and charts.
 * Uses React.lazy + Suspense for code splitting with skeleton fallback.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const DashboardContent = lazy(() =>
  import('./-components/DashboardContent').then((mod) => ({
    default: mod.DashboardContent,
  }))
);

export const Route = createFileRoute('/_authenticated/dashboard')({
  component: DashboardPage,
});

function DashboardPage() {
  return (
    <Suspense fallback={<PageSkeleton title="Dashboard" />}>
      <DashboardContent />
    </Suspense>
  );
}
