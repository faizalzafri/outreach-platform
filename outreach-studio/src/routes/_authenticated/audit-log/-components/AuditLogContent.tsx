/**
 * Audit Log Content (lazy-loaded)
 *
 * Placeholder for the audit log page implementation.
 */

import { Route } from '../index';

export function AuditLogContent() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>Audit Log</h1>
      <p>
        Page {search.page}, Size {search.size}
        {search.user && `, User: ${search.user}`}
        {search.action && `, Action: ${search.action}`}
        {search.resourceType && `, Resource: ${search.resourceType}`}
      </p>
    </div>
  );
}
