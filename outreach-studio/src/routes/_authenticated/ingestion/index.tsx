/**
 * Ingestion Route
 *
 * Provides Excel/CSV upload with drag-and-drop and import job tracking.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { IngestionContent } from './-components/IngestionContent';

const ingestionSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  status: z
    .enum(['PENDING', 'IN_PROGRESS', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED'])
    .optional()
    .catch(undefined),
});

export type IngestionSearch = z.infer<typeof ingestionSearchSchema>;

export const Route = createFileRoute('/_authenticated/ingestion/')({
  validateSearch: (search: Record<string, unknown>): IngestionSearch =>
    ingestionSearchSchema.parse(search),
  component: IngestionPage,
});

function IngestionPage() {
  return (
    <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
      <IngestionContent />
    </ProtectedRoute>
  );
}
