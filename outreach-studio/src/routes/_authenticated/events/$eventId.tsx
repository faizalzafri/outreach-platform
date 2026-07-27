/**
 * Event Detail Route
 *
 * Displays details for a single event with lifecycle transition controls.
 * Supports nested tabs: Overview, Volunteers, Feedback, Notifications, Audit History.
 * Uses React.lazy + Suspense for code splitting.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { z } from 'zod';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const EventDetailContent = lazy(() =>
  import('./-components/EventDetailContent').then((mod) => ({
    default: mod.EventDetailContent,
  }))
);

const eventDetailSearchSchema = z.object({
  tab: z
    .enum(['overview', 'volunteers', 'feedback', 'notifications', 'audit'])
    .default('overview')
    .catch('overview'),
});

export type EventDetailSearch = z.infer<typeof eventDetailSearchSchema>;

export const Route = createFileRoute('/_authenticated/events/$eventId')({
  validateSearch: (search: Record<string, unknown>): EventDetailSearch =>
    eventDetailSearchSchema.parse(search),
  component: EventDetailPage,
});

function EventDetailPage() {
  return (
    <Suspense fallback={<PageSkeleton title="Event Detail" />}>
      <EventDetailContent />
    </Suspense>
  );
}
