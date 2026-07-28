/**
 * Feedback Route
 *
 * Role-based rendering:
 * - ROLE_ADMIN / ROLE_PMO: Read-only feedback listing (DataTable)
 * - ROLE_POC: Feedback submission form
 *
 * Validates search params with Zod schema using .catch() to discard invalid params.
 * Uses React.lazy + Suspense for code splitting.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { z } from 'zod';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';
import { useAuth } from '@/hooks/useAuth';

const FeedbackContent = lazy(() =>
  import('./-components/FeedbackContent').then((mod) => ({
    default: mod.FeedbackContent,
  }))
);

const FeedbackListContent = lazy(() =>
  import('./-components/FeedbackListContent').then((mod) => ({
    default: mod.FeedbackListContent,
  }))
);

const feedbackSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  eventId: z.string().optional().catch(undefined),
});

export type FeedbackSearch = z.infer<typeof feedbackSearchSchema>;

export const Route = createFileRoute('/_authenticated/feedback/')({
  validateSearch: (search: Record<string, unknown>): FeedbackSearch =>
    feedbackSearchSchema.parse(search),
  component: FeedbackPage,
});

function FeedbackPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];

  const isAdminOrPmo =
    roles.includes('ROLE_ADMIN') || roles.includes('ROLE_PMO');

  return (
    <Suspense fallback={<PageSkeleton title="Feedback" />}>
      {isAdminOrPmo ? <FeedbackListContent /> : <FeedbackContent />}
    </Suspense>
  );
}
