/**
 * Volunteer List Route
 *
 * Displays paginated, searchable list of volunteers.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 * Uses React.lazy + Suspense for code splitting.
 *
 * Requirements: 3.2, 3.4, 3.5
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { z } from 'zod';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const VolunteerListContent = lazy(() =>
  import('./-components/VolunteerListContent').then((mod) => ({
    default: mod.VolunteerListContent,
  }))
);

const volunteerListSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  sort: z.string().optional().catch(undefined),
  search: z.string().optional().catch(undefined),
});

export type VolunteerListSearch = z.infer<typeof volunteerListSearchSchema>;

export const Route = createFileRoute('/_authenticated/volunteers/')({
  validateSearch: (search: Record<string, unknown>): VolunteerListSearch =>
    volunteerListSearchSchema.parse(search),
  component: VolunteerListPage,
});

function VolunteerListPage() {
  return (
    <Suspense fallback={<PageSkeleton title="Volunteers" />}>
      <VolunteerListContent />
    </Suspense>
  );
}
