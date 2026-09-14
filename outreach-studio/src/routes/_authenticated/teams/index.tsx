/**
 * Teams Route
 *
 * Team management list page, restricted to ADMIN-level roles. Validates
 * search params with Zod using .catch() to discard invalid params, and uses
 * React.lazy + Suspense for route-level code splitting.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { teamListSearchSchema, type TeamListSearch } from '@/lib/zod-schemas';

const TeamListContent = lazy(() =>
  import('./-components/TeamListContent').then((mod) => ({
    default: mod.TeamListContent,
  }))
);

export const Route = createFileRoute('/_authenticated/teams/')({
  validateSearch: (search: Record<string, unknown>): TeamListSearch =>
    teamListSearchSchema.parse(search),
  component: TeamsPage,
});

const TEAM_MANAGEMENT_ROLES = ['ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_PLATFORM_ADMIN'];

function TeamsPage() {
  return (
    <ProtectedRoute requiredRoles={TEAM_MANAGEMENT_ROLES}>
      <Suspense fallback={<PageSkeleton title="Teams" />}>
        <TeamListContent />
      </Suspense>
    </ProtectedRoute>
  );
}
