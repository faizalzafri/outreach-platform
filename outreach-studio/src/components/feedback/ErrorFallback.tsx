/**
 * Error Fallback UI
 *
 * Displays a user-friendly error summary with a retry button.
 * Stack traces and error codes are hidden behind an expandable "Details" section.
 */

import { useState } from 'react';
import styles from './ErrorFallback.module.css';

interface ErrorFallbackProps {
  error: Error | null;
  onRetry: () => void;
}

export function ErrorFallback({ error, onRetry }: ErrorFallbackProps) {
  const [showDetails, setShowDetails] = useState(false);

  const errorMessage = error?.message || 'An unexpected error occurred.';

  return (
    <div className={styles.container} role="alert" aria-live="assertive">
      <span className={styles.icon} aria-hidden="true">⚠</span>
      <h2 className={styles.title}>Something went wrong</h2>
      <p className={styles.message}>
        We encountered an error while loading this page. Please try again.
      </p>
      <button
        type="button"
        className={styles.retryButton}
        onClick={onRetry}
      >
        Retry
      </button>

      {error && (
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
              {errorMessage}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
