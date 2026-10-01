/**
 * Security Route
 *
 * The organization's password and one-time-code policy, for tenant admins.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';

const SecurityPolicyContent = lazy(() =>
  import('./-components/SecurityPolicyContent').then((mod) => ({
    default: mod.SecurityPolicyContent,
  }))
);

export const Route = createFileRoute('/_authenticated/admin/security/')({
  component: SecurityPage,
});

function SecurityPage() {
  return (
    <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
      <Suspense fallback={<PageSkeleton title="Sign-in security" />}>
        <SecurityPolicyContent />
      </Suspense>
    </ProtectedRoute>
  );
}
