/**
 * Admin Content (lazy-loaded)
 *
 * Placeholder for the user administration page implementation.
 * Actual user management UI will be implemented in task 19.
 */

import { Route } from '../index';

export function AdminContent() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>User Administration</h1>
      <p>
        Page {search.page}, Size {search.size}
        {search.role && `, Role: ${search.role}`}
        {search.status && `, Status: ${search.status}`}
      </p>
    </div>
  );
}
