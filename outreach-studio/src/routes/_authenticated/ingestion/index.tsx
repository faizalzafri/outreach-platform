/**
 * Ingestion Route
 *
 * Provides Excel/CSV upload with drag-and-drop and import job tracking.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 * Uses React.lazy + Suspense for code splitting.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { z } from 'zod';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const IngestionContent = lazy(() =>
  import('./-components/IngestionContent').then((mod) => ({
    default: mod.IngestionContent,
  }))
);

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
    <Suspense fallback={<PageSkeleton title="Data Ingestion" />}>
      <IngestionContent />
    </Suspense>
  );
}
