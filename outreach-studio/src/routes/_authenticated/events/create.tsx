/**
 * Event Create Route
 *
 * Form for creating a new event with Zod validation.
 * Uses React.lazy + Suspense for code splitting.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';

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
    <ProtectedRoute requiredRoles={['ROLE_PMO', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_PLATFORM_ADMIN']}>
      <Suspense fallback={<PageSkeleton title="Create Event" />}>
        <EventCreateContent />
      </Suspense>
    </ProtectedRoute>
  );
}
