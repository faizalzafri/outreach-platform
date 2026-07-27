/**
 * Event List Route
 *
 * Displays paginated, filterable list of events.
 * Validates search params with Zod schema, discarding invalid params and applying defaults.
 *
 * Requirements: 3.2, 3.4, 3.5
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';

const eventListSearchSchema = z.object({
  page: z.number().int().positive().default(1),
  size: z.number().int().positive().default(10),
  sort: z.string().optional(),
  status: z
    .enum(['DRAFT', 'PUBLISHED', 'ACTIVE', 'COMPLETED', 'ARCHIVED', 'CANCELLED'])
    .optional(),
  search: z.string().optional(),
});

type EventListSearch = z.infer<typeof eventListSearchSchema>;

export const Route = createFileRoute('/_authenticated/events/')({
  validateSearch: (search: Record<string, unknown>): EventListSearch => {
    const result = eventListSearchSchema.safeParse(search);
    if (result.success) {
      return result.data;
    }
    // Discard invalid params, apply defaults
    return eventListSearchSchema.parse({});
  },
  component: EventListPage,
});

function EventListPage() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>Events</h1>
      <p>
        Page {search.page}, Size {search.size}
        {search.status && `, Status: ${search.status}`}
        {search.search && `, Search: ${search.search}`}
      </p>
    </div>
  );
}
