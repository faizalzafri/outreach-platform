/**
 * Organizations Route (platform console)
 *
 * Platform admins list, search and create organizations.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';

const TenantListContent = lazy(() =>
  import('./-components/TenantListContent').then((mod) => ({
    default: mod.TenantListContent,
  }))
);

export const Route = createFileRoute('/_authenticated/platform/tenants/')({
  component: TenantsPage,
});

function TenantsPage() {
  return (
    <ProtectedRoute requiredRoles={['ROLE_PLATFORM_ADMIN']}>
      <Suspense fallback={<PageSkeleton title="Organizations" />}>
        <TenantListContent />
      </Suspense>
    </ProtectedRoute>
  );
}
