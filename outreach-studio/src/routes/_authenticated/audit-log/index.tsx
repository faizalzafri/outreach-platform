/**
 * Audit Log Route
 *
 * Displays audit trail entries with filters and expandable JSON payloads.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 * Uses React.lazy + Suspense for code splitting.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { z } from 'zod';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';

const AuditLogContent = lazy(() =>
  import('./-components/AuditLogContent').then((mod) => ({
    default: mod.AuditLogContent,
  }))
);

const auditLogSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(50).catch(50),
  user: z.string().optional().catch(undefined),
  action: z.string().optional().catch(undefined),
  resourceType: z.string().optional().catch(undefined),
  startDate: z.string().optional().catch(undefined),
  endDate: z.string().optional().catch(undefined),
});

export type AuditLogSearch = z.infer<typeof auditLogSearchSchema>;

export const Route = createFileRoute('/_authenticated/audit-log/')({
  validateSearch: (search: Record<string, unknown>): AuditLogSearch =>
    auditLogSearchSchema.parse(search),
  component: AuditLogPage,
});

function AuditLogPage() {
  return (
    <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
      <Suspense fallback={<PageSkeleton title="Audit Log" />}>
        <AuditLogContent />
      </Suspense>
    </ProtectedRoute>
  );
}
