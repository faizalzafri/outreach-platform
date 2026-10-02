/**
 * Reports Route
 *
 * Displays reports with filters, charts, and export functionality.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { ReportsContent } from './-components/ReportsContent';

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
    <ReportsContent />
  );
}
