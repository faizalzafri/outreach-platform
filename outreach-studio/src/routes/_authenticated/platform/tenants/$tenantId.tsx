/**
 * Organization Detail Route (platform console)
 *
 * One organization: rename, change status, manage its people and its sign-in security.
 */

import { createFileRoute } from '@tanstack/react-router';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { TenantDetailContent } from './-components/TenantDetailContent';

export const Route = createFileRoute('/_authenticated/platform/tenants/$tenantId')({
  component: TenantDetailPage,
});

function TenantDetailPage() {
  const { tenantId } = Route.useParams();
  return (
    <ProtectedRoute requiredRoles={['ROLE_PLATFORM_ADMIN']}>
      <TenantDetailContent tenantId={tenantId} />
    </ProtectedRoute>
  );
}
