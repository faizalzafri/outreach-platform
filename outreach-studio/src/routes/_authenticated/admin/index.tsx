/**
 * Admin Route
 *
 * User administration page with role management and account controls.
 *
 * Requirements: 3.2, 3.3
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';

const adminSearchSchema = z.object({
  page: z.number().int().positive().default(1),
  size: z.number().int().positive().default(10),
  role: z.enum(['ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC']).optional(),
  status: z.enum(['ENABLED', 'DISABLED', 'LOCKED']).optional(),
});

type AdminSearch = z.infer<typeof adminSearchSchema>;

export const Route = createFileRoute('/_authenticated/admin/')({
  validateSearch: (search: Record<string, unknown>): AdminSearch => {
    const result = adminSearchSchema.safeParse(search);
    if (result.success) {
      return result.data;
    }
    return adminSearchSchema.parse({});
  },
  component: AdminPage,
});

function AdminPage() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>User Administration</h1>
      <p>
        Page {search.page}, Size {search.size}
        {search.role && `, Role: ${search.role}`}
        {search.status && `, Status: ${search.status}`}
      </p>
    </div>
  );
}
