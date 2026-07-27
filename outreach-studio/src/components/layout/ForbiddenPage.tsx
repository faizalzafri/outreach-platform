/**
 * Forbidden Page (403)
 *
 * Displayed when a user attempts to access a route they are not authorized for.
 * Shows a clear message and a link back to the dashboard.
 *
 * Requirements: 9.3, 9.9
 */

import styles from './ForbiddenPage.module.css';

export function ForbiddenPage() {
  return (
    <div className={styles.container} role="alert" aria-labelledby="forbidden-heading">
      <div className={styles.content}>
        <span className={styles.errorCode} aria-hidden="true">
          403
        </span>
        <h1 id="forbidden-heading" className={styles.heading}>
          Access Denied
        </h1>
        <p className={styles.message}>
          You do not have permission to view this page. Please contact your
          administrator if you believe this is an error.
        </p>
        <a href="/dashboard" className={styles.link}>
          ← Return to Dashboard
        </a>
      </div>
    </div>
  );
}
