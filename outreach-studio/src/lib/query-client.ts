import { QueryClient } from '@tanstack/react-query';

/**
 * Global TanStack Query client with sensible defaults for the Outreach FMS SPA.
 *
 * - staleTime: 30s — data is considered fresh for 30 seconds before background refetch
 * - gcTime: 5min — unused query data is garbage-collected after 5 minutes
 * - refetchOnWindowFocus: true — refetch stale queries when the browser tab regains focus
 * - retry: 3 — failed queries are retried up to 3 times with exponential backoff
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60 * 1000,
      refetchOnWindowFocus: true,
      retry: 3,
    },
  },
});
