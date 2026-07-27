import { useCallback, useEffect, useRef } from 'react';

import { setToastHandler } from '@/lib/http-client';
import { useUIStore } from '@/stores/ui-store';
import type { ToastNotification } from '@/stores/ui-store';
import type { ToastPayload } from '@/types/api';

import styles from './ToastContainer.module.css';

/** Maximum number of toasts rendered simultaneously. */
const MAX_VISIBLE = 5;

/** Auto-dismiss delay in milliseconds for non-error toasts. */
const AUTO_DISMISS_MS = 5_000;

/**
 * Renders the global toast notification stack.
 *
 * - Displays up to 5 toasts at once (most recent first visually at bottom).
 * - Auto-dismisses success, warning, and info toasts after 5 seconds.
 * - Error toasts persist until manually closed.
 * - Displays correlation ID on error toasts for debugging.
 * - Wires the HTTP client's toast handler on mount so API-level toasts
 *   (e.g. 429 rate-limit warnings) flow through the store.
 */
export function ToastContainer() {
  const toasts = useUIStore((s) => s.toasts);
  const addToast = useUIStore((s) => s.addToast);
  const dismissToast = useUIStore((s) => s.dismissToast);

  // Wire the HTTP client's pluggable toast handler on mount.
  useEffect(() => {
    const handler = (payload: ToastPayload) => {
      addToast({
        severity: payload.severity,
        message: payload.message,
        correlationId: payload.correlationId,
      });
    };
    setToastHandler(handler);

    // No cleanup — the handler persists for the app's lifetime.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Show only the most recent MAX_VISIBLE toasts.
  const visibleToasts = toasts.slice(-MAX_VISIBLE);

  return (
    <div
      className={styles.container}
      role="region"
      aria-label="Notifications"
      aria-live="polite"
    >
      {visibleToasts.map((toast) => (
        <ToastItem
          key={toast.id}
          toast={toast}
          onDismiss={dismissToast}
        />
      ))}
    </div>
  );
}

// --- ToastItem ---

interface ToastItemProps {
  toast: ToastNotification;
  onDismiss: (id: string) => void;
}

function ToastItem({ toast, onDismiss }: ToastItemProps) {
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleDismiss = useCallback(() => {
    onDismiss(toast.id);
  }, [onDismiss, toast.id]);

  // Auto-dismiss non-error toasts after 5 seconds.
  useEffect(() => {
    if (toast.severity === 'error') {
      return; // Error toasts persist until manual close.
    }

    timerRef.current = setTimeout(handleDismiss, AUTO_DISMISS_MS);

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, [toast.severity, handleDismiss]);

  const severityClass = styles[toast.severity] ?? '';

  return (
    <div
      className={`${styles.toast} ${severityClass}`}
      role="alert"
      aria-atomic="true"
    >
      <div className={styles.content}>
        <span className={styles.icon} aria-hidden="true">
          {getSeverityIcon(toast.severity)}
        </span>
        <div className={styles.body}>
          <p className={styles.message}>{toast.message}</p>
          {toast.severity === 'error' && toast.correlationId && (
            <p className={styles.correlationId}>
              ID: {toast.correlationId}
            </p>
          )}
        </div>
      </div>
      <button
        type="button"
        className={styles.closeButton}
        onClick={handleDismiss}
        aria-label="Dismiss notification"
      >
        ×
      </button>
    </div>
  );
}

function getSeverityIcon(severity: ToastNotification['severity']): string {
  switch (severity) {
    case 'success':
      return '✓';
    case 'error':
      return '✕';
    case 'warning':
      return '⚠';
    case 'info':
      return 'ℹ';
  }
}
