/**
 * Connectivity Monitor
 *
 * Renders the appropriate banner (offline or system unavailable)
 * based on current connectivity state. Also wires up the offline
 * request queue with proper callbacks.
 */

import { useEffect } from 'react';

import { useConnectivity } from '@/hooks/useConnectivity';
import { offlineQueue } from '@/lib/offline-queue';
import { useUIStore } from '@/stores/ui-store';
import { httpClient } from '@/lib/http-client';
import type { NormalizedError } from '@/types/api';
import type { QueuedRequest } from '@/lib/offline-queue';
import { OfflineBanner } from './OfflineBanner';
import { SystemUnavailableBanner } from './SystemUnavailableBanner';

export function ConnectivityMonitor() {
  const { isOffline, isSystemUnavailable } = useConnectivity();
  const addToast = useUIStore((s) => s.addToast);

  // Configure the offline queue with real handlers
  useEffect(() => {
    offlineQueue.configure({
      executeRequest: async (request: QueuedRequest) => {
        await httpClient.request({
          method: request.method,
          url: request.url,
          data: request.data,
          headers: request.headers,
        });
      },
      onRequestDiscarded: (_request: QueuedRequest, error: NormalizedError) => {
        addToast({
          severity: 'error',
          message: `Failed to sync request after multiple retries: ${error.message}`,
          correlationId: error.correlationId ?? undefined,
        });
      },
      onQueueFlushed: () => {
        // Toast is already shown by useConnectivity on reconnect
      },
    });
  }, [addToast]);

  if (isSystemUnavailable) {
    return <SystemUnavailableBanner />;
  }

  if (isOffline) {
    return <OfflineBanner />;
  }

  return null;
}
