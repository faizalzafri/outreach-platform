/**
 * Feedback Route
 *
 * Displays feedback list and submission form.
 *
 * Requirements: 3.2, 3.3
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';

const feedbackSearchSchema = z.object({
  page: z.number().int().positive().default(1),
  size: z.number().int().positive().default(10),
  eventId: z.string().optional(),
});

type FeedbackSearch = z.infer<typeof feedbackSearchSchema>;

export const Route = createFileRoute('/_authenticated/feedback/')({
  validateSearch: (search: Record<string, unknown>): FeedbackSearch => {
    const result = feedbackSearchSchema.safeParse(search);
    if (result.success) {
      return result.data;
    }
    return feedbackSearchSchema.parse({});
  },
  component: FeedbackPage,
});

function FeedbackPage() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>Feedback</h1>
      <p>
        Page {search.page}, Size {search.size}
        {search.eventId && `, Event: ${search.eventId}`}
      </p>
    </div>
  );
}
