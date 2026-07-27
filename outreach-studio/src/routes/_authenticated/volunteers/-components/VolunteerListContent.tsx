/**
 * Volunteer List Content (lazy-loaded)
 *
 * Placeholder for the volunteer list page implementation.
 * Actual DataTable with search will be implemented in task 13.
 */

import { Route } from '../index';

export function VolunteerListContent() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>Volunteers</h1>
      <p>
        Page {search.page}, Size {search.size}
        {search.search && `, Search: ${search.search}`}
      </p>
    </div>
  );
}
