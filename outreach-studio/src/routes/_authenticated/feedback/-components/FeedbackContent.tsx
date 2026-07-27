/**
 * Feedback Content (lazy-loaded)
 *
 * Placeholder for the feedback page implementation.
 * Actual feedback form and list will be implemented in task 14.
 */

import { Route } from '../index';

export function FeedbackContent() {
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
