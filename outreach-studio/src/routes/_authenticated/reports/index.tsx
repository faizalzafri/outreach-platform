/**
 * Reports Route
 *
 * Displays reports with filters, charts, and export functionality.
 *
 * Requirements: 3.2, 3.3
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';

const reportSearchSchema = z.object({
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  granularity: z.enum(['DAY', 'WEEK', 'MONTH', 'QUARTER']).default('DAY'),
  tab: z.enum(['event', 'beneficiary', 'city', 'poc']).default('event'),
});

type ReportSearch = z.infer<typeof reportSearchSchema>;

export const Route = createFileRoute('/_authenticated/reports/')({
  validateSearch: (search: Record<string, unknown>): ReportSearch => {
    const result = reportSearchSchema.safeParse(search);
    if (result.success) {
      return result.data;
    }
    return reportSearchSchema.parse({});
  },
  component: ReportsPage,
});

function ReportsPage() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>Reports</h1>
      <p>
        Granularity: {search.granularity}, Tab: {search.tab}
        {search.startDate && `, From: ${search.startDate}`}
        {search.endDate && `, To: ${search.endDate}`}
      </p>
    </div>
  );
}
