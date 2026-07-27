/**
 * Event Detail Content (lazy-loaded)
 *
 * Placeholder for the event detail page implementation.
 */

import { Route } from '../$eventId';

export function EventDetailContent() {
  const { eventId } = Route.useParams();
  const { tab } = Route.useSearch();

  return (
    <div>
      <h1>Event Detail</h1>
      <p>Event ID: {eventId}</p>
      <p>Active Tab: {tab}</p>
    </div>
  );
}
