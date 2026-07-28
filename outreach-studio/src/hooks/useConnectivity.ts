/**
 * Connectivity Detection Hook
 *
 * Monitors online/offline status using:
 * - window 'online'/'offline' events for immediate detection
 * - HEAD /api/health confirmation to validate actual API reachability
 * - System unavailable detection (API unreachable > 10s)
 *
 * On reconnect: dismisses banner within 3s, flushes offline queue,
 * shows confirmation toast.
 */

import { useEffect, useRef, useCallback, useState } from 'react';

import { useUIStore } from '@/stores/ui-store';
import { offlineQueue } from '@/lib/offline-queue';

const HEALTH_CHECK_URL = '/api/health';
const HEALTH_CHECK_TIMEOUT = 5_000;
const RECONNECT_DISMISS_DELAY = 3_000;
const SYSTEM_UNAVAILABLE_THRESHOLD = 10_000;

export interface ConnectivityState {
  isOffline: boolean;
  isSystemUnavailable: boolean;
}

/**
 * Performs a lightweight HEAD request to confirm API reachability.
 */
async function checkApiHealth(): Promise<boolean> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), HEALTH_CHECK_TIMEOUT);

    const response = await fetch(HEALTH_CHECK_URL, {
      method: 'HEAD',
      signal: controller.signal,
      cache: 'no-store',
    });

    clearTimeout(timeoutId);
    return response.ok || response.status < 500;
  } catch {
    return false;
  }
}

/**
 * Hook that manages offline detection and system unavailability.
 * Integrates with the UIStore for offline state and the offline request queue.
 */
export function useConnectivity(): ConnectivityState {
  const setOffline = useUIStore((s) => s.setOffline);
  const isOffline = useUIStore((s) => s.isOffline);
  const addToast = useUIStore((s) => s.addToast);

  const [isSystemUnavailable, setIsSystemUnavailable] = useState(false);
  const unavailableSinceRef = useRef<number | null>(null);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const healthCheckIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const handleOnline = useCallback(async () => {
    const apiReachable = await checkApiHealth();

    if (apiReachable) {
      // Delay banner dismissal by up to 3s for visual confirmation
      reconnectTimerRef.current = setTimeout(() => {
        setOffline(false);
        setIsSystemUnavailable(false);
        unavailableSinceRef.current = null;

        // Flush queued requests
        void offlineQueue.flush();

        addToast({
          severity: 'success',
          message: 'Connection restored. Pending requests are being processed.',
        });
      }, Math.min(RECONNECT_DISMISS_DELAY, 3_000));
    }
  }, [setOffline, addToast]);

  const handleOffline = useCallback(() => {
    setOffline(true);
  }, [setOffline]);

  const performHealthCheck = useCallback(async () => {
    const reachable = await checkApiHealth();

    if (!reachable) {
      if (!unavailableSinceRef.current) {
        unavailableSinceRef.current = Date.now();
      }

      const elapsed = Date.now() - unavailableSinceRef.current;
      if (elapsed >= SYSTEM_UNAVAILABLE_THRESHOLD) {
        setIsSystemUnavailable(true);
        setOffline(true);
      }
    } else {
      if (isOffline && unavailableSinceRef.current) {
        // System was unavailable but is now reachable — treat as reconnect
        void handleOnline();
      }
      unavailableSinceRef.current = null;
      setIsSystemUnavailable(false);
    }
  }, [isOffline, setOffline, handleOnline]);

  useEffect(() => {
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Periodic health check every 10s when offline
    healthCheckIntervalRef.current = setInterval(() => {
      if (isOffline || !navigator.onLine) {
        void performHealthCheck();
      }
    }, 10_000);

    // Initial offline check
    if (!navigator.onLine) {
      handleOffline();
    }

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);

      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
      }
      if (healthCheckIntervalRef.current) {
        clearInterval(healthCheckIntervalRef.current);
      }
    };
  }, [handleOnline, handleOffline, isOffline, performHealthCheck]);

  return {
    isOffline,
    isSystemUnavailable,
  };
}
