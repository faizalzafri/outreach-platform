/**
 * Error Details Component
 *
 * Displays human-readable error messages from server responses.
 * Status codes, correlation IDs, and technical details are hidden
 * behind an expandable "Details" section.
 */

import { useState } from 'react';
import type { NormalizedError } from '@/types/api';
import styles from './ErrorFallback.module.css';

interface ErrorDetailsProps {
  error: NormalizedError;
}

export function ErrorDetails({ error }: ErrorDetailsProps) {
  const [showDetails, setShowDetails] = useState(false);

  return (
    <div role="alert" aria-live="polite">
      <p className={styles.message}>{error.message}</p>

      <div className={styles.details}>
        <button
          type="button"
          className={styles.detailsToggle}
          onClick={() => setShowDetails((prev) => !prev)}
          aria-expanded={showDetails}
        >
          {showDetails ? '▾' : '▸'} Details
        </button>
        {showDetails && (
          <div className={styles.detailsContent}>
            {`Status: ${error.status}\nType: ${error.type}${error.correlationId ? `\nCorrelation ID: ${error.correlationId}` : ''}`}
            {error.fieldErrors.length > 0 && (
              <>
                {'\n\nField Errors:'}
                {error.fieldErrors.map((fe) => `\n  ${fe.field}: ${fe.message}`).join('')}
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
