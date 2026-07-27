/**
 * Notifications Content (lazy-loaded)
 *
 * Placeholder for the notifications page implementation.
 * Actual template list and delivery management will be implemented in task 17.
 */

import { Route } from '../index';

export function NotificationsContent() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>Notifications</h1>
      <p>
        Page {search.page}, Size {search.size}
        {search.type && `, Type: ${search.type}`}
      </p>
    </div>
  );
}
