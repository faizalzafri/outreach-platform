/**
 * Ingestion Content (lazy-loaded)
 *
 * Placeholder for the data ingestion page implementation.
 */

import { Route } from '../index';

export function IngestionContent() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>Data Ingestion</h1>
      <p>
        Upload Excel/CSV files for bulk data import.
        {` Page ${search.page}, Size ${search.size}`}
        {search.status && `, Status: ${search.status}`}
      </p>
    </div>
  );
}
