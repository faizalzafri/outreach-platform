/**
 * Organization Detail Route (platform console)
 *
 * One organization: rename, change status, manage its people and its sign-in security.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';

const TenantDetailContent = lazy(() =>
  import('./-components/TenantDetailContent').then((mod) => ({
    default: mod.TenantDetailContent,
  }))
);

export const Route = createFileRoute('/_authenticated/platform/tenants/$tenantId')({
  component: TenantDetailPage,
});

function TenantDetailPage() {
  const { tenantId } = Route.useParams();
  return (
    <ProtectedRoute requiredRoles={['ROLE_PLATFORM_ADMIN']}>
      <Suspense fallback={<PageSkeleton title="Organization" />}>
        <TenantDetailContent tenantId={tenantId} />
      </Suspense>
    </ProtectedRoute>
  );
}
