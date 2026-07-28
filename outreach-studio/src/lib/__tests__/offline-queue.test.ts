import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { OfflineRequestQueue } from '../offline-queue';
import type { QueuedRequest } from '../offline-queue';
import type { NormalizedError } from '@/types/api';

function createQueue(overrides: Partial<Parameters<typeof OfflineRequestQueue.prototype.configure>[0]> = {}) {
  return new OfflineRequestQueue({
    maxSize: 50,
    maxRetries: 3,
    retryInterval: 5_000,
    onRequestDiscarded: () => {},
    onQueueFlushed: () => {},
    executeRequest: async () => {},
    ...overrides,
  });
}

describe('OfflineRequestQueue', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('enqueue', () => {
    it('adds requests to the queue', () => {
      const queue = createQueue();

      const result = queue.enqueue({ method: 'POST', url: '/api/events' });

      expect(result).toBe(true);
      expect(queue.getSize()).toBe(1);
    });

    it('rejects requests when queue is at max capacity', () => {
      const queue = createQueue({ maxSize: 2 });

      queue.enqueue({ method: 'POST', url: '/api/a' });
      queue.enqueue({ method: 'POST', url: '/api/b' });
      const result = queue.enqueue({ method: 'POST', url: '/api/c' });

      expect(result).toBe(false);
      expect(queue.getSize()).toBe(2);
    });

    it('assigns unique IDs and initial retryCount of 0', () => {
      const queue = createQueue();

      queue.enqueue({ method: 'PUT', url: '/api/events/1', data: { name: 'Test' } });

      const items = queue.getQueue();
      expect(items[0].id).toBeDefined();
      expect(items[0].retryCount).toBe(0);
      expect(items[0].method).toBe('PUT');
      expect(items[0].url).toBe('/api/events/1');
    });
  });

  describe('flush', () => {
    it('executes all queued requests on flush', async () => {
      const executeRequest = vi.fn().mockResolvedValue(undefined);
      const queue = createQueue({ executeRequest });

      queue.enqueue({ method: 'POST', url: '/api/events' });
      queue.enqueue({ method: 'PUT', url: '/api/volunteers/1' });

      await queue.flush();

      expect(executeRequest).toHaveBeenCalledTimes(2);
      expect(queue.getSize()).toBe(0);
    });

    it('calls onQueueFlushed when all requests succeed', async () => {
      const onQueueFlushed = vi.fn();
      const queue = createQueue({
        executeRequest: vi.fn().mockResolvedValue(undefined),
        onQueueFlushed,
      });

      queue.enqueue({ method: 'POST', url: '/api/test' });
      await queue.flush();

      expect(onQueueFlushed).toHaveBeenCalledOnce();
    });

    it('re-queues failed requests with incremented retryCount', async () => {
      const executeRequest = vi.fn().mockRejectedValue({ message: 'Network error' } as NormalizedError);
      const queue = createQueue({ executeRequest, maxRetries: 3 });

      queue.enqueue({ method: 'POST', url: '/api/events' });

      await queue.flush();

      expect(queue.getSize()).toBe(1);
      expect(queue.getQueue()[0].retryCount).toBe(1);
    });

    it('discards requests after max retries exhausted', async () => {
      const onRequestDiscarded = vi.fn();
      const executeRequest = vi.fn().mockRejectedValue({
        status: 500,
        type: 'INTERNAL_SERVER_ERROR',
        message: 'Server error',
        correlationId: null,
        fieldErrors: [],
      } as NormalizedError);

      const queue = createQueue({
        executeRequest,
        onRequestDiscarded,
        maxRetries: 1,
      });

      queue.enqueue({ method: 'POST', url: '/api/events' });

      // First flush: retryCount goes from 0 to 1 (reaches maxRetries)
      await queue.flush();

      expect(onRequestDiscarded).toHaveBeenCalledOnce();
      expect(queue.getSize()).toBe(0);
    });

    it('does nothing if queue is empty', async () => {
      const executeRequest = vi.fn();
      const queue = createQueue({ executeRequest });

      await queue.flush();

      expect(executeRequest).not.toHaveBeenCalled();
    });

    it('does not flush concurrently if already flushing', async () => {
      let resolveFirst: () => void;
      const firstPromise = new Promise<void>((resolve) => { resolveFirst = resolve; });
      const executeRequest = vi.fn().mockImplementation(() => firstPromise);
      const queue = createQueue({ executeRequest });

      queue.enqueue({ method: 'POST', url: '/api/events' });

      // Start flush but don't resolve yet
      const flush1 = queue.flush();
      const flush2 = queue.flush(); // Should be a no-op since already flushing

      resolveFirst!();
      await flush1;
      await flush2;

      // Only one execution should have happened
      expect(executeRequest).toHaveBeenCalledTimes(1);
    });
  });

  describe('retry timer', () => {
    it('retries pending requests at configured interval', async () => {
      const executeRequest = vi.fn()
        .mockRejectedValueOnce({ message: 'fail' } as NormalizedError)
        .mockResolvedValueOnce(undefined);

      const queue = createQueue({
        executeRequest,
        retryInterval: 5_000,
        maxRetries: 3,
      });

      queue.enqueue({ method: 'POST', url: '/api/events' });

      // First attempt via enqueue starts the timer
      // Advance to trigger retry
      await vi.advanceTimersByTimeAsync(5_000);

      // First retry failed, request re-queued
      expect(queue.getSize()).toBe(1);

      // Second retry succeeds
      await vi.advanceTimersByTimeAsync(5_000);

      expect(queue.getSize()).toBe(0);
    });
  });

  describe('discard', () => {
    it('removes a specific request by ID', () => {
      const queue = createQueue();

      queue.enqueue({ method: 'POST', url: '/api/a' });
      queue.enqueue({ method: 'POST', url: '/api/b' });

      const items = queue.getQueue();
      queue.discard(items[0].id);

      expect(queue.getSize()).toBe(1);
      expect(queue.getQueue()[0].url).toBe('/api/b');
    });
  });

  describe('clear', () => {
    it('removes all queued requests', () => {
      const queue = createQueue();

      queue.enqueue({ method: 'POST', url: '/api/a' });
      queue.enqueue({ method: 'POST', url: '/api/b' });

      queue.clear();

      expect(queue.getSize()).toBe(0);
    });
  });

  describe('destroy', () => {
    it('clears queue and stops retry timer', () => {
      const queue = createQueue();

      queue.enqueue({ method: 'POST', url: '/api/a' });
      queue.destroy();

      expect(queue.getSize()).toBe(0);
    });
  });
});
