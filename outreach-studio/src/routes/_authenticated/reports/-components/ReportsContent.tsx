/**
 * Reports Content (lazy-loaded)
 *
 * Placeholder for the reports page implementation.
 * Actual charts, filters, and export will be implemented in task 18.
 */

import { Route } from '../index';

export function ReportsContent() {
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
