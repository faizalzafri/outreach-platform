/**
 * System Unavailable Banner
 *
 * Non-dismissible banner displayed when the API Gateway is unreachable
 * for more than 10 seconds. Shows a different message from the offline
 * banner to indicate the server itself is down.
 */

import styles from './OfflineBanner.module.css';

export function SystemUnavailableBanner() {
  return (
    <div
      className={`${styles.banner} ${styles.unavailable}`}
      role="alert"
      aria-live="assertive"
      aria-atomic="true"
    >
      <span className={styles.icon} aria-hidden="true">🔴</span>
      <p className={styles.message}>
        System unavailable. The server is not responding. Please try again later.
      </p>
    </div>
  );
}
