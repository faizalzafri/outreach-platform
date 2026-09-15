/**
 * Team Detail Route
 *
 * Displays a single team's details and member roster, restricted to
 * ADMIN-level roles. Supports `?edit=true` to open directly in edit mode
 * (used by the "Edit" row action on the team list page).
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { teamDetailSearchSchema, type TeamDetailSearch } from '@/lib/zod-schemas';

const TeamDetailContent = lazy(() =>
  import('./-components/TeamDetailContent').then((mod) => ({
    default: mod.TeamDetailContent,
  }))
);

export const Route = createFileRoute('/_authenticated/teams/$teamId')({
  validateSearch: (search: Record<string, unknown>): TeamDetailSearch =>
    teamDetailSearchSchema.parse(search),
  component: TeamDetailPage,
});

const TEAM_MANAGEMENT_ROLES = ['ROLE_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_PLATFORM_ADMIN'];

function TeamDetailPage() {
  return (
    <ProtectedRoute requiredRoles={TEAM_MANAGEMENT_ROLES}>
      <Suspense fallback={<PageSkeleton title="Team" />}>
        <TeamDetailContent />
      </Suspense>
    </ProtectedRoute>
  );
}
