/**
 * Volunteer Detail Route
 *
 * Displays profile, participation history, and feedback score trends
 * for a specific volunteer.
 * Uses React.lazy + Suspense for code splitting.
 *
 * Requirements: 3.2, 3.3
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const VolunteerDetailContent = lazy(() =>
  import('./-components/VolunteerDetailContent').then((mod) => ({
    default: mod.VolunteerDetailContent,
  }))
);

export const Route = createFileRoute('/_authenticated/volunteers/$employeeId')({
  component: VolunteerDetailPage,
});

function VolunteerDetailPage() {
  return (
    <Suspense fallback={<PageSkeleton title="Volunteer Detail" />}>
      <VolunteerDetailContent />
    </Suspense>
  );
}
