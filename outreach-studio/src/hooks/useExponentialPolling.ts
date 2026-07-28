/**
 * Hook for polling with exponential backoff.
 *
 * Implements polling that starts at a base interval and doubles each cycle,
 * capped at a maximum interval. Useful for async job status polling
 * (e.g., import jobs, export jobs).
 *
 * Backoff schedule with defaults: 3s, 6s, 12s, 24s, 30s (capped), 30s, ...
 */

import { useState, useRef, useCallback, useEffect } from 'react';

interface ExponentialPollingOptions<T> {
  /** Function that fetches the current status */
  queryFn: () => Promise<T>;
  /** Predicate that returns true when polling should stop */
  isTerminal: (data: T) => boolean;
  /** Base interval in milliseconds. Default: 3000 */
  baseInterval?: number;
  /** Maximum interval in milliseconds. Default: 30000 */
  maxInterval?: number;
  /** Maximum number of poll attempts. Default: 60 */
  maxAttempts?: number;
  /** Whether polling is enabled */
  enabled?: boolean;
  /** Callback on each successful poll */
  onData?: (data: T) => void;
  /** Callback when polling reaches terminal state */
  onComplete?: (data: T) => void;
  /** Callback on poll error */
  onError?: (error: unknown) => void;
  /** Callback when max attempts exceeded */
  onTimeout?: () => void;
}

interface ExponentialPollingState<T> {
  /** Latest polled data */
  data: T | null;
  /** Whether currently polling */
  isPolling: boolean;
  /** Number of poll attempts made */
  attemptCount: number;
  /** Whether polling timed out (max attempts reached) */
  timedOut: boolean;
  /** Current interval being used */
  currentInterval: number;
  /** Error from last poll attempt, if any */
  error: unknown | null;
  /** Start polling */
  start: () => void;
  /** Stop polling manually */
  stop: () => void;
}

export function useExponentialPolling<T>({
  queryFn,
  isTerminal,
  baseInterval = 3000,
  maxInterval = 30000,
  maxAttempts = 60,
  enabled = false,
  onData,
  onComplete,
  onError,
  onTimeout,
}: ExponentialPollingOptions<T>): ExponentialPollingState<T> {
  const [data, setData] = useState<T | null>(null);
  const [isPolling, setIsPolling] = useState(false);
  const [attemptCount, setAttemptCount] = useState(0);
  const [timedOut, setTimedOut] = useState(false);
  const [currentInterval, setCurrentInterval] = useState(baseInterval);
  const [error, setError] = useState<unknown | null>(null);

  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isActiveRef = useRef(false);
  const attemptRef = useRef(0);
  const intervalRef = useRef(baseInterval);
  const pollRef = useRef<() => Promise<void>>();

  const cleanup = useCallback(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
    isActiveRef.current = false;
  }, []);

  const poll = useCallback(async () => {
    if (!isActiveRef.current) return;

    if (attemptRef.current >= maxAttempts) {
      setIsPolling(false);
      setTimedOut(true);
      cleanup();
      onTimeout?.();
      return;
    }

    attemptRef.current += 1;
    setAttemptCount(attemptRef.current);

    try {
      const result = await queryFn();
      if (!isActiveRef.current) return;

      setData(result);
      setError(null);
      onData?.(result);

      if (isTerminal(result)) {
        setIsPolling(false);
        cleanup();
        onComplete?.(result);
        return;
      }

      // Schedule next poll with exponential backoff
      const nextInterval = Math.min(intervalRef.current * 2, maxInterval);
      intervalRef.current = nextInterval;
      setCurrentInterval(nextInterval);

      timeoutRef.current = setTimeout(() => void pollRef.current?.(), intervalRef.current);
    } catch (err) {
      if (!isActiveRef.current) return;
      setError(err);
      onError?.(err);

      // Continue polling on error (with backoff)
      const nextInterval = Math.min(intervalRef.current * 2, maxInterval);
      intervalRef.current = nextInterval;
      setCurrentInterval(nextInterval);

      timeoutRef.current = setTimeout(() => void pollRef.current?.(), intervalRef.current);
    }
  }, [queryFn, isTerminal, maxAttempts, maxInterval, cleanup, onData, onComplete, onError, onTimeout]);

  // Keep pollRef in sync with the latest poll callback
  useEffect(() => {
    pollRef.current = poll;
  });

  const start = useCallback(() => {
    cleanup();
    isActiveRef.current = true;
    attemptRef.current = 0;
    intervalRef.current = baseInterval;
    setAttemptCount(0);
    setTimedOut(false);
    setIsPolling(true);
    setCurrentInterval(baseInterval);
    setError(null);

    // First poll at the base interval
    timeoutRef.current = setTimeout(() => void pollRef.current?.(), baseInterval);
  }, [cleanup, baseInterval, poll]);

  const stop = useCallback(() => {
    cleanup();
    setIsPolling(false);
  }, [cleanup]);

  // Auto-start when enabled becomes true
  useEffect(() => {
    if (enabled && !isActiveRef.current) {
      start();
    } else if (!enabled && isActiveRef.current) {
      stop();
    }
  }, [enabled, start, stop]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      cleanup();
    };
  }, [cleanup]);

  return {
    data,
    isPolling,
    attemptCount,
    timedOut,
    currentInterval,
    error,
    start,
    stop,
  };
}
