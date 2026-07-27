/**
 * Event List Content (lazy-loaded)
 *
 * Placeholder for the event list page implementation.
 * Actual DataTable with event data will be implemented in task 12.
 */

import { Route } from '../index';

export function EventListContent() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>Events</h1>
      <p>
        Page {search.page}, Size {search.size}
        {search.status && `, Status: ${search.status}`}
        {search.search && `, Search: ${search.search}`}
      </p>
    </div>
  );
}
