/**
 * Organizations Route (platform console)
 *
 * Platform admins list, search and create organizations.
 */

import { createFileRoute } from '@tanstack/react-router';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { TenantListContent } from './-components/TenantListContent';

export const Route = createFileRoute('/_authenticated/platform/tenants/')({
  component: TenantsPage,
});

function TenantsPage() {
  return (
    <ProtectedRoute requiredRoles={['ROLE_PLATFORM_ADMIN']}>
      <TenantListContent />
    </ProtectedRoute>
  );
}
