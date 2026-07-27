/**
 * Audit Log Route
 *
 * Displays audit trail entries with filters and expandable JSON payloads.
 *
 * Requirements: 3.2, 3.3
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';

const auditLogSearchSchema = z.object({
  page: z.number().int().positive().default(1),
  size: z.number().int().positive().default(50),
  user: z.string().optional(),
  action: z.string().optional(),
  resourceType: z.string().optional(),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
});

type AuditLogSearch = z.infer<typeof auditLogSearchSchema>;

export const Route = createFileRoute('/_authenticated/audit-log/')({
  validateSearch: (search: Record<string, unknown>): AuditLogSearch => {
    const result = auditLogSearchSchema.safeParse(search);
    if (result.success) {
      return result.data;
    }
    return auditLogSearchSchema.parse({});
  },
  component: AuditLogPage,
});

function AuditLogPage() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>Audit Log</h1>
      <p>
        Page {search.page}, Size {search.size}
        {search.user && `, User: ${search.user}`}
        {search.action && `, Action: ${search.action}`}
        {search.resourceType && `, Resource: ${search.resourceType}`}
      </p>
    </div>
  );
}
