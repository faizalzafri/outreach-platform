/**
 * Offline Request Queue
 *
 * Queues failed requests while the app is offline and retries them on reconnect.
 * - Max queue size: 50 requests
 * - Retry interval: 5 seconds
 * - Max retries per request: 3
 * - On reconnect: flushes all queued requests
 * - Discards requests after max retries, triggers error notification
 */

import type { NormalizedError } from '@/types/api';

export interface QueuedRequest {
  id: string;
  method: string;
  url: string;
  data?: unknown;
  headers?: Record<string, string>;
  retryCount: number;
  createdAt: number;
}

export interface OfflineQueueConfig {
  maxSize: number;
  maxRetries: number;
  retryInterval: number;
  onRequestDiscarded: (request: QueuedRequest, error: NormalizedError) => void;
  onQueueFlushed: () => void;
  executeRequest: (request: QueuedRequest) => Promise<void>;
}

const DEFAULT_CONFIG: OfflineQueueConfig = {
  maxSize: 50,
  maxRetries: 3,
  retryInterval: 5_000,
  onRequestDiscarded: () => {},
  onQueueFlushed: () => {},
  executeRequest: async () => {},
};

class OfflineRequestQueue {
  private queue: QueuedRequest[] = [];
  private config: OfflineQueueConfig;
  private retryTimer: ReturnType<typeof setInterval> | null = null;
  private isFlushing = false;

  constructor(config: Partial<OfflineQueueConfig> = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config };
  }

  configure(config: Partial<OfflineQueueConfig>): void {
    this.config = { ...this.config, ...config };
  }

  enqueue(request: Omit<QueuedRequest, 'id' | 'retryCount' | 'createdAt'>): boolean {
    if (this.queue.length >= this.config.maxSize) {
      return false;
    }

    const queuedRequest: QueuedRequest = {
      ...request,
      id: generateId(),
      retryCount: 0,
      createdAt: Date.now(),
    };

    this.queue.push(queuedRequest);
    this.startRetryTimer();
    return true;
  }

  async flush(): Promise<void> {
    if (this.isFlushing || this.queue.length === 0) return;

    this.isFlushing = true;
    this.stopRetryTimer();

    const requestsToProcess = [...this.queue];
    this.queue = [];

    for (const request of requestsToProcess) {
      try {
        await this.config.executeRequest(request);
      } catch (error) {
        request.retryCount++;

        if (request.retryCount >= this.config.maxRetries) {
          this.config.onRequestDiscarded(request, error as NormalizedError);
        } else {
          this.queue.push(request);
        }
      }
    }

    this.isFlushing = false;

    if (this.queue.length > 0) {
      this.startRetryTimer();
    } else {
      this.config.onQueueFlushed();
    }
  }

  discard(requestId: string): void {
    this.queue = this.queue.filter((r) => r.id !== requestId);
    if (this.queue.length === 0) {
      this.stopRetryTimer();
    }
  }

  getQueue(): ReadonlyArray<QueuedRequest> {
    return this.queue;
  }

  getSize(): number {
    return this.queue.length;
  }

  clear(): void {
    this.queue = [];
    this.stopRetryTimer();
  }

  private startRetryTimer(): void {
    if (this.retryTimer) return;

    this.retryTimer = setInterval(() => {
      void this.retryPending();
    }, this.config.retryInterval);
  }

  private stopRetryTimer(): void {
    if (this.retryTimer) {
      clearInterval(this.retryTimer);
      this.retryTimer = null;
    }
  }

  private async retryPending(): Promise<void> {
    if (this.isFlushing || this.queue.length === 0) return;

    this.isFlushing = true;
    const requestsToRetry = [...this.queue];
    this.queue = [];

    for (const request of requestsToRetry) {
      try {
        await this.config.executeRequest(request);
      } catch (error) {
        request.retryCount++;

        if (request.retryCount >= this.config.maxRetries) {
          this.config.onRequestDiscarded(request, error as NormalizedError);
        } else {
          this.queue.push(request);
        }
      }
    }

    this.isFlushing = false;

    if (this.queue.length === 0) {
      this.stopRetryTimer();
    }
  }

  destroy(): void {
    this.stopRetryTimer();
    this.queue = [];
  }
}

function generateId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

export const offlineQueue = new OfflineRequestQueue();
export { OfflineRequestQueue };
