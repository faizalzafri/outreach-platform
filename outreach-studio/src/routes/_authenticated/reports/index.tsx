/**
 * Reports Route
 *
 * Displays reports with filters, charts, and export functionality.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 * Uses React.lazy + Suspense for code splitting.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { z } from 'zod';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const ReportsContent = lazy(() =>
  import('./-components/ReportsContent').then((mod) => ({
    default: mod.ReportsContent,
  }))
);

const reportSearchSchema = z.object({
  startDate: z.string().optional().catch(undefined),
  endDate: z.string().optional().catch(undefined),
  granularity: z.enum(['DAY', 'WEEK', 'MONTH', 'QUARTER']).default('DAY').catch('DAY'),
  tab: z.enum(['event', 'beneficiary', 'city', 'poc']).default('event').catch('event'),
});

export type ReportSearch = z.infer<typeof reportSearchSchema>;

export const Route = createFileRoute('/_authenticated/reports/')({
  validateSearch: (search: Record<string, unknown>): ReportSearch =>
    reportSearchSchema.parse(search),
  component: ReportsPage,
});

function ReportsPage() {
  return (
    <Suspense fallback={<PageSkeleton title="Reports" />}>
      <ReportsContent />
    </Suspense>
  );
}
