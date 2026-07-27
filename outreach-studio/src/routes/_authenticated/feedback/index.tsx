/**
 * Feedback Route
 *
 * Displays feedback list and submission form.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 * Uses React.lazy + Suspense for code splitting.
 *
 * Requirements: 3.2, 3.4, 3.5
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { z } from 'zod';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const FeedbackContent = lazy(() =>
  import('./-components/FeedbackContent').then((mod) => ({
    default: mod.FeedbackContent,
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
  return (
    <Suspense fallback={<PageSkeleton title="Feedback" />}>
      <FeedbackContent />
    </Suspense>
  );
}
