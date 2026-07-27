import { useCallback } from 'react';

import { useUIStore } from '@/stores/ui-store';
import type { ToastNotification } from '@/stores/ui-store';

export type ToastOptions = Omit<ToastNotification, 'id' | 'timestamp'>;

export interface UseToastReturn {
  /** Enqueue a toast notification with the given severity and message. */
  toast: (options: ToastOptions) => void;
  /** Convenience: enqueue a success toast. */
  success: (message: string) => void;
  /** Convenience: enqueue an error toast with optional correlation ID. */
  error: (message: string, correlationId?: string) => void;
  /** Convenience: enqueue a warning toast. */
  warning: (message: string) => void;
  /** Convenience: enqueue an info toast. */
  info: (message: string) => void;
  /** Dismiss a toast by its ID. */
  dismiss: (id: string) => void;
}

/**
 * Hook for enqueueing toast notifications.
 *
 * Wraps the Zustand UI store's `addToast` / `dismissToast` actions
 * with a convenient API including severity-specific helpers.
 */
export function useToast(): UseToastReturn {
  const addToast = useUIStore((s) => s.addToast);
  const dismissToast = useUIStore((s) => s.dismissToast);

  const toast = useCallback(
    (options: ToastOptions) => {
      addToast(options);
    },
    [addToast],
  );

  const success = useCallback(
    (message: string) => {
      addToast({ severity: 'success', message });
    },
    [addToast],
  );

  const error = useCallback(
    (message: string, correlationId?: string) => {
      addToast({ severity: 'error', message, correlationId });
    },
    [addToast],
  );

  const warning = useCallback(
    (message: string) => {
      addToast({ severity: 'warning', message });
    },
    [addToast],
  );

  const info = useCallback(
    (message: string) => {
      addToast({ severity: 'info', message });
    },
    [addToast],
  );

  const dismiss = useCallback(
    (id: string) => {
      dismissToast(id);
    },
    [dismissToast],
  );

  return { toast, success, error, warning, info, dismiss };
}
