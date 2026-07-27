/**
 * Event List Route
 *
 * Displays paginated, filterable list of events.
 * Validates search params with Zod schema using .catch() to discard invalid params
 * and apply defaults gracefully.
 * Uses React.lazy + Suspense for code splitting.
 *
 * Requirements: 3.2, 3.4, 3.5
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { z } from 'zod';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const EventListContent = lazy(() =>
  import('./-components/EventListContent').then((mod) => ({
    default: mod.EventListContent,
  }))
);

const eventListSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  sort: z.string().optional().catch(undefined),
  status: z
    .enum(['DRAFT', 'PUBLISHED', 'ACTIVE', 'COMPLETED', 'ARCHIVED', 'CANCELLED'])
    .optional()
    .catch(undefined),
  search: z.string().optional().catch(undefined),
});

export type EventListSearch = z.infer<typeof eventListSearchSchema>;

export const Route = createFileRoute('/_authenticated/events/')({
  validateSearch: (search: Record<string, unknown>): EventListSearch =>
    eventListSearchSchema.parse(search),
  component: EventListPage,
});

function EventListPage() {
  return (
    <Suspense fallback={<PageSkeleton title="Events" />}>
      <EventListContent />
    </Suspense>
  );
}
