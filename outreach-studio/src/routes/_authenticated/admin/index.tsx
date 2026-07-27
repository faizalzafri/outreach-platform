/**
 * Admin Route
 *
 * User administration page with role management and account controls.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 * Uses React.lazy + Suspense for code splitting.
 *
 * Requirements: 3.2, 3.4, 3.5
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { z } from 'zod';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const AdminContent = lazy(() =>
  import('./-components/AdminContent').then((mod) => ({
    default: mod.AdminContent,
  }))
);

const adminSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  role: z.enum(['ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC']).optional().catch(undefined),
  status: z.enum(['ENABLED', 'DISABLED', 'LOCKED']).optional().catch(undefined),
});

export type AdminSearch = z.infer<typeof adminSearchSchema>;

export const Route = createFileRoute('/_authenticated/admin/')({
  validateSearch: (search: Record<string, unknown>): AdminSearch =>
    adminSearchSchema.parse(search),
  component: AdminPage,
});

function AdminPage() {
  return (
    <Suspense fallback={<PageSkeleton title="User Administration" />}>
      <AdminContent />
    </Suspense>
  );
}
