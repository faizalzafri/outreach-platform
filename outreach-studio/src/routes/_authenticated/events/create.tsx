/**
 * Event Create Route
 *
 * Form for creating a new event with Zod validation.
 * Uses React.lazy + Suspense for code splitting.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const EventCreateContent = lazy(() =>
  import('./-components/EventCreateContent').then((mod) => ({
    default: mod.EventCreateContent,
  }))
);

export const Route = createFileRoute('/_authenticated/events/create')({
  component: EventCreatePage,
});

function EventCreatePage() {
  return (
    <Suspense fallback={<PageSkeleton title="Create Event" />}>
      <EventCreateContent />
    </Suspense>
  );
}
