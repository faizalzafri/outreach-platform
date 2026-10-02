/**
 * Volunteer List Route
 *
 * Displays paginated, searchable list of volunteers.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { VolunteerListContent } from './-components/VolunteerListContent';

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
    <VolunteerListContent />
  );
}
