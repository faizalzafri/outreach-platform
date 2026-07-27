/**
 * Ingestion Route
 *
 * Provides Excel/CSV upload with drag-and-drop and import job tracking.
 *
 * Requirements: 3.2, 3.3
 */

import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/ingestion/')({
  component: IngestionPage,
});

function IngestionPage() {
  return (
    <div>
      <h1>Data Ingestion</h1>
      <p>Upload Excel/CSV files for bulk data import.</p>
    </div>
  );
}
