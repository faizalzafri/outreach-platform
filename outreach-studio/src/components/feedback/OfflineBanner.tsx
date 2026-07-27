/**
 * Offline Banner
 *
 * Persistent banner displayed when the app detects offline status.
 * Non-dismissible — disappears automatically within 3s of connectivity restoration.
 */

import styles from './OfflineBanner.module.css';

export function OfflineBanner() {
  return (
    <div
      className={`${styles.banner} ${styles.offline}`}
      role="alert"
      aria-live="assertive"
      aria-atomic="true"
    >
      <span className={styles.icon} aria-hidden="true">⚡</span>
      <p className={styles.message}>
        You are offline. Changes will be saved and synced when your connection is restored.
      </p>
    </div>
  );
}
