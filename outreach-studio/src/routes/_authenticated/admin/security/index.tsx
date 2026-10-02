/**
 * Security Route
 *
 * The organization's password and one-time-code policy, for tenant admins.
 */

import { createFileRoute } from '@tanstack/react-router';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { SecurityPolicyContent } from './-components/SecurityPolicyContent';

export const Route = createFileRoute('/_authenticated/admin/security/')({
  component: SecurityPage,
});

function SecurityPage() {
  return (
    <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
      <SecurityPolicyContent />
    </ProtectedRoute>
  );
}
