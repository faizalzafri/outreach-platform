/**
 * Event Create Route
 *
 * Form for creating a new event with Zod validation.
 */

import { createFileRoute } from '@tanstack/react-router';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { EventCreateContent } from './-components/EventCreateContent';

export const Route = createFileRoute('/_authenticated/events/create')({
  component: EventCreatePage,
});

function EventCreatePage() {
  return (
    <ProtectedRoute requiredRoles={['ROLE_PMO', 'ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_PLATFORM_ADMIN']}>
      <EventCreateContent />
    </ProtectedRoute>
  );
}
