/**
 * Hook for managing row-level loading states in DataTable action buttons.
 *
 * Provides per-row loading indicators without blocking other rows,
 * enabling concurrent actions on different table rows.
 */

import { useState, useCallback } from 'react';

interface RowActionState {
  /** Set of row IDs currently in a loading state */
  loadingRows: Set<string>;
  /** Check if a specific row is loading */
  isRowLoading: (rowId: string) => boolean;
  /** Execute an action for a row, managing its loading state */
  executeRowAction: <T>(rowId: string, action: () => Promise<T>) => Promise<T>;
}

export function useRowAction(): RowActionState {
  const [loadingRows, setLoadingRows] = useState<Set<string>>(new Set());

  const isRowLoading = useCallback(
    (rowId: string) => loadingRows.has(rowId),
    [loadingRows],
  );

  const executeRowAction = useCallback(
    async <T>(rowId: string, action: () => Promise<T>): Promise<T> => {
      setLoadingRows((prev) => new Set(prev).add(rowId));
      try {
        const result = await action();
        return result;
      } finally {
        setLoadingRows((prev) => {
          const next = new Set(prev);
          next.delete(rowId);
          return next;
        });
      }
    },
    [],
  );

  return { loadingRows, isRowLoading, executeRowAction };
}
