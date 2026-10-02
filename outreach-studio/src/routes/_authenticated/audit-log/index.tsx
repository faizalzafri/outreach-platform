/**
 * Audit Log Route
 *
 * Displays audit trail entries with filters and expandable JSON payloads.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { AuditLogContent } from './-components/AuditLogContent';

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
      <AuditLogContent />
    </ProtectedRoute>
  );
}
