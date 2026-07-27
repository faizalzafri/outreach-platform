/**
 * Volunteer List Route
 *
 * Displays paginated, searchable list of volunteers.
 * Validates search params with Zod schema.
 *
 * Requirements: 3.2, 3.4, 3.5
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';

const volunteerListSearchSchema = z.object({
  page: z.number().int().positive().default(1),
  size: z.number().int().positive().default(10),
  sort: z.string().optional(),
  search: z.string().optional(),
});

type VolunteerListSearch = z.infer<typeof volunteerListSearchSchema>;

export const Route = createFileRoute('/_authenticated/volunteers/')({
  validateSearch: (search: Record<string, unknown>): VolunteerListSearch => {
    const result = volunteerListSearchSchema.safeParse(search);
    if (result.success) {
      return result.data;
    }
    return volunteerListSearchSchema.parse({});
  },
  component: VolunteerListPage,
});

function VolunteerListPage() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>Volunteers</h1>
      <p>
        Page {search.page}, Size {search.size}
        {search.search && `, Search: ${search.search}`}
      </p>
    </div>
  );
}
