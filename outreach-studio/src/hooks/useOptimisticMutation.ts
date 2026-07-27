/**
 * Hook for creating optimistic mutations with automatic timeout-based rollback.
 *
 * Provides a consistent pattern for:
 * - Applying optimistic state immediately
 * - Rolling back within a specified timeout if the server doesn't respond
 * - Rolling back on server rejection
 */

import { useRef, useCallback } from 'react';
import { useMutation, useQueryClient, type QueryKey } from '@tanstack/react-query';
import type { NormalizedError } from '@/types/api';

interface OptimisticMutationOptions<TData, TVariables> {
  /** Mutation function that calls the API */
  mutationFn: (variables: TVariables) => Promise<TData>;
  /** Query key to update optimistically */
  queryKey: QueryKey;
  /** Function to apply the optimistic update to cached data */
  optimisticUpdate: (cached: TData | undefined, variables: TVariables) => TData | undefined;
  /** Maximum time (ms) to wait for server response before rollback. Default: 1000ms */
  rollbackTimeout?: number;
  /** Callback invoked after successful mutation */
  onSuccess?: (data: TData) => void;
  /** Callback invoked on error, receives error message */
  onError?: (errorMessage: string) => void;
  /** Additional query keys to invalidate after success */
  invalidateKeys?: QueryKey[];
}

export function useOptimisticMutation<TData, TVariables>({
  mutationFn,
  queryKey,
  optimisticUpdate,
  rollbackTimeout = 1000,
  onSuccess,
  onError,
  invalidateKeys = [],
}: OptimisticMutationOptions<TData, TVariables>) {
  const queryClient = useQueryClient();
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const rollbackRef = useRef<TData | undefined>(undefined);
  const hasRolledBackRef = useRef(false);

  const mutation = useMutation<TData, NormalizedError, TVariables>({
    mutationFn,
    onMutate: (variables) => {
      hasRolledBackRef.current = false;

      // Snapshot previous data for rollback
      const previousData = queryClient.getQueryData<TData>(queryKey);
      rollbackRef.current = previousData;

      // Apply optimistic update
      const updated = optimisticUpdate(previousData, variables);
      if (updated !== undefined) {
        queryClient.setQueryData(queryKey, updated);
      }

      // Start rollback timeout
      if (rollbackTimeout > 0) {
        timeoutRef.current = setTimeout(() => {
          // Only rollback if the mutation is still pending
          if (!hasRolledBackRef.current && mutation.isPending) {
            hasRolledBackRef.current = true;
            queryClient.setQueryData(queryKey, rollbackRef.current);
            onError?.('Request timed out. Please try again.');
          }
        }, rollbackTimeout);
      }

      return { previousData };
    },
    onSuccess: (data) => {
      hasRolledBackRef.current = true;
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = null;
      }

      // Update cache with server response
      queryClient.setQueryData(queryKey, data);

      // Invalidate related queries
      for (const key of invalidateKeys) {
        void queryClient.invalidateQueries({ queryKey: key });
      }

      onSuccess?.(data);
    },
    onError: (err) => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = null;
      }

      // Rollback if not already done by timeout
      if (!hasRolledBackRef.current) {
        hasRolledBackRef.current = true;
        queryClient.setQueryData(queryKey, rollbackRef.current);
      }

      onError?.(err.message || 'Operation failed. Please try again.');
    },
  });

  const mutate = useCallback(
    (variables: TVariables) => {
      mutation.mutate(variables);
    },
    [mutation],
  );

  return {
    mutate,
    isPending: mutation.isPending,
    isError: mutation.isError,
    error: mutation.error,
  };
}
