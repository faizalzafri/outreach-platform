/**
 * Audit Log Content (lazy-loaded)
 *
 * Displays audit trail entries with:
 * - Filters: date range pickers, user text input (300ms debounce),
 *   action type select, resource type select
 * - DataTable with columns: timestamp, user, action, resource type,
 *   resource ID, IP address (sorted by timestamp desc)
 * - Expandable rows showing full JSON payload in formatted read-only view
 * - CSV export via GET /api/admin/audit-log/export with current filters
 * - Cursor-based pagination (default 50 entries) with "Load More" control
 * - Error state with retry button; empty state message
 */

import { useState, useCallback, useMemo } from 'react';
import { useInfiniteQuery } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useDebounce } from '@/hooks/useDebounce';
import { useToast } from '@/hooks/useToast';
import type { AuditEntry } from '@/types/domain';
import type { NormalizedError } from '@/types/api';

import { Route } from '../index';
import styles from './AuditLogContent.module.css';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const DEFAULT_PAGE_SIZE = 50;

const ACTION_OPTIONS = [
  { label: 'Create', value: 'CREATE' },
  { label: 'Update', value: 'UPDATE' },
  { label: 'Delete', value: 'DELETE' },
  { label: 'Login', value: 'LOGIN' },
  { label: 'Logout', value: 'LOGOUT' },
] as const;

const RESOURCE_TYPE_OPTIONS = [
  { label: 'Event', value: 'EVENT' },
  { label: 'Volunteer', value: 'VOLUNTEER' },
  { label: 'Feedback', value: 'FEEDBACK' },
  { label: 'User', value: 'USER' },
  { label: 'Notification', value: 'NOTIFICATION' },
  { label: 'Import Job', value: 'IMPORT_JOB' },
] as const;

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface CursorPageResponse {
  content: AuditEntry[];
  nextCursor: string | null;
  hasMore: boolean;
  totalElements?: number;
}

type SortDirection = 'asc' | 'desc' | null;

interface SortState {
  column: string;
  direction: SortDirection;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatTimestamp(isoString: string): string {
  return new Date(isoString).toLocaleString();
}

// ---------------------------------------------------------------------------
// Row Group Sub-component (handles expandable row pairs)
// ---------------------------------------------------------------------------

interface RowGroupProps {
  entry: AuditEntry;
  isExpanded: boolean;
  onToggle: (id: string) => void;
  colSpan: number;
}

function RowGroup({ entry, isExpanded, onToggle, colSpan }: RowGroupProps) {
  return (
    <>
      <tr
        className={`${styles['tr']} ${isExpanded ? styles['trExpanded'] : ''}`}
        onClick={() => onToggle(entry.id)}
        aria-expanded={isExpanded}
      >
        <td className={styles['td']}>{formatTimestamp(entry.timestamp)}</td>
        <td className={styles['td']}>{entry.user}</td>
        <td className={styles['td']}>{entry.action}</td>
        <td className={styles['td']}>{entry.resourceType}</td>
        <td className={styles['td']}>{entry.resourceId}</td>
        <td className={styles['td']}>{entry.ipAddress}</td>
      </tr>
      {isExpanded && (
        <tr className={styles['expandedRow']}>
          <td colSpan={colSpan} className={styles['expandedCell']}>
            <div className={styles['payloadContainer']}>
              <pre className={styles['payloadPre']}>
                {JSON.stringify(entry.payload, null, 2)}
              </pre>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function AuditLogContent() {
  const search = Route.useSearch();
  const { error: toastError } = useToast();

  // --- Filter state ---
  const [startDate, setStartDate] = useState(search.startDate ?? '');
  const [endDate, setEndDate] = useState(search.endDate ?? '');
  const [userFilter, setUserFilter] = useState(search.user ?? '');
  const [actionFilter, setActionFilter] = useState(search.action ?? '');
  const [resourceTypeFilter, setResourceTypeFilter] = useState(search.resourceType ?? '');

  // Debounce user text filter by 300ms
  const debouncedUser = useDebounce(userFilter, 300);

  // --- Sort state (default: timestamp desc) ---
  const [sortState, setSortState] = useState<SortState>({
    column: 'timestamp',
    direction: 'desc',
  });

  // --- Row expansion state ---
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  // --- Export loading state ---
  const [exporting, setExporting] = useState(false);

  // --- Build filter params ---
  const filterParams = useMemo(() => {
    const params: Record<string, string> = {};
    if (startDate) params['startDate'] = startDate;
    if (endDate) params['endDate'] = endDate;
    if (debouncedUser) params['user'] = debouncedUser;
    if (actionFilter) params['action'] = actionFilter;
    if (resourceTypeFilter) params['resourceType'] = resourceTypeFilter;
    if (sortState.direction) {
      params['sort'] = `${sortState.column},${sortState.direction}`;
    }
    params['size'] = String(DEFAULT_PAGE_SIZE);
    return params;
  }, [startDate, endDate, debouncedUser, actionFilter, resourceTypeFilter, sortState]);

  // --- Cursor-based pagination with TanStack Query useInfiniteQuery ---
  const {
    data,
    isLoading,
    isError,
    error,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    refetch,
  } = useInfiniteQuery<CursorPageResponse, NormalizedError>({
    queryKey: [...queryKeys.admin.auditLog(filterParams), 'infinite'],
    queryFn: async ({ pageParam }) => {
      const params: Record<string, string> = { ...filterParams };
      if (pageParam) {
        params['cursor'] = pageParam as string;
      }
      const response = await httpClient.get<CursorPageResponse>('/admin/audit-log', {
        params,
      });
      return response.data;
    },
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? (lastPage.nextCursor ?? undefined) : undefined,
  });

  // Flatten pages into a single list
  const entries = useMemo(
    () => data?.pages.flatMap((page) => page.content) ?? [],
    [data],
  );

  const totalElements = data?.pages[0]?.totalElements;

  // --- Sorting handler ---
  const handleSort = useCallback((column: string) => {
    setSortState((prev) => {
      if (prev.column !== column) {
        return { column, direction: 'asc' };
      }
      // Cycle: asc → desc → null (unsorted)
      if (prev.direction === 'asc') return { column, direction: 'desc' };
      if (prev.direction === 'desc') return { column: 'timestamp', direction: 'desc' };
      return { column, direction: 'asc' };
    });
  }, []);

  // --- Row expansion toggle ---
  const toggleRow = useCallback((id: string) => {
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }, []);

  // --- CSV Export ---
  const handleExport = useCallback(async () => {
    setExporting(true);
    try {
      const params: Record<string, string> = {};
      if (startDate) params['startDate'] = startDate;
      if (endDate) params['endDate'] = endDate;
      if (debouncedUser) params['user'] = debouncedUser;
      if (actionFilter) params['action'] = actionFilter;
      if (resourceTypeFilter) params['resourceType'] = resourceTypeFilter;

      const response = await httpClient.get('/admin/audit-log/export', {
        params,
        responseType: 'blob',
      });

      // Create download link
      const blob = new Blob([response.data as BlobPart], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `audit-log-export-${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      const normalized = err as NormalizedError;
      toastError(normalized.message || 'Failed to export audit log');
    } finally {
      setExporting(false);
    }
  }, [startDate, endDate, debouncedUser, actionFilter, resourceTypeFilter, toastError]);

  // --- Sort indicator ---
  const getSortIndicator = (column: string) => {
    if (sortState.column !== column || !sortState.direction) return '↕';
    return sortState.direction === 'asc' ? '↑' : '↓';
  };

  const isSortActive = (column: string) =>
    sortState.column === column && sortState.direction !== null;

  // --- Column definitions ---
  const sortableColumns = [
    { id: 'timestamp', label: 'Timestamp' },
    { id: 'user', label: 'User' },
    { id: 'action', label: 'Action' },
    { id: 'resourceType', label: 'Resource Type' },
    { id: 'resourceId', label: 'Resource ID' },
    { id: 'ipAddress', label: 'IP Address' },
  ] as const;

  return (
    <div className={styles['container']}>
      {/* Header */}
      <div className={styles['header']}>
        <h1 className={styles['pageTitle']}>Audit Log</h1>
        <button
          type="button"
          className={styles['exportBtn']}
          onClick={() => void handleExport()}
          disabled={exporting}
          aria-busy={exporting}
        >
          {exporting ? 'Exporting...' : 'Export CSV'}
        </button>
      </div>

      {/* Filters */}
      <div className={styles['filters']}>
        <div className={styles['filterGroup']}>
          <label className={styles['filterLabel']} htmlFor="filter-start-date">
            Start Date
          </label>
          <input
            id="filter-start-date"
            type="date"
            className={styles['filterInput']}
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />
        </div>

        <div className={styles['filterGroup']}>
          <label className={styles['filterLabel']} htmlFor="filter-end-date">
            End Date
          </label>
          <input
            id="filter-end-date"
            type="date"
            className={styles['filterInput']}
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
        </div>

        <div className={styles['filterGroup']}>
          <label className={styles['filterLabel']} htmlFor="filter-user">
            User
          </label>
          <input
            id="filter-user"
            type="text"
            className={styles['filterInput']}
            placeholder="Filter by user..."
            value={userFilter}
            onChange={(e) => setUserFilter(e.target.value)}
          />
        </div>

        <div className={styles['filterGroup']}>
          <label className={styles['filterLabel']} htmlFor="filter-action">
            Action
          </label>
          <select
            id="filter-action"
            className={styles['filterSelect']}
            value={actionFilter}
            onChange={(e) => setActionFilter(e.target.value)}
          >
            <option value="">All Actions</option>
            {ACTION_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        <div className={styles['filterGroup']}>
          <label className={styles['filterLabel']} htmlFor="filter-resource-type">
            Resource Type
          </label>
          <select
            id="filter-resource-type"
            className={styles['filterSelect']}
            value={resourceTypeFilter}
            onChange={(e) => setResourceTypeFilter(e.target.value)}
          >
            <option value="">All Types</option>
            {RESOURCE_TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Table */}
      <div className={styles['tableWrapper']}>
        <table className={styles['table']}>
          <thead className={styles['thead']}>
            <tr>
              {sortableColumns.map((col) => (
                <th
                  key={col.id}
                  className={`${styles['th']} ${styles['thSortable']}`}
                  onClick={() => handleSort(col.id)}
                  aria-sort={
                    sortState.column === col.id && sortState.direction === 'asc'
                      ? 'ascending'
                      : sortState.column === col.id && sortState.direction === 'desc'
                        ? 'descending'
                        : 'none'
                  }
                  scope="col"
                >
                  <span className={styles['thContent']}>
                    {col.label}
                    <span
                      className={`${styles['sortIndicator']} ${isSortActive(col.id) ? styles['sortIndicatorActive'] : ''}`}
                      aria-hidden="true"
                    >
                      {getSortIndicator(col.id)}
                    </span>
                  </span>
                </th>
              ))}
            </tr>
          </thead>

          <tbody>
            {/* Loading skeleton */}
            {isLoading && (
              <>
                {Array.from({ length: 10 }, (_, i) => (
                  <tr key={`skeleton-${String(i)}`} className={styles['skeletonRow']}>
                    {sortableColumns.map((col) => (
                      <td key={col.id} className={styles['skeletonCell']}>
                        <div className={styles['skeletonBar']} />
                      </td>
                    ))}
                  </tr>
                ))}
              </>
            )}

            {/* Error state */}
            {!isLoading && isError && (
              <tr>
                <td colSpan={sortableColumns.length}>
                  <div className={styles['errorState']}>
                    <p className={styles['errorMessage']}>
                      {(error as NormalizedError)?.message ?? 'Failed to load audit log entries.'}
                    </p>
                    <button
                      type="button"
                      className={styles['retryBtn']}
                      onClick={() => void refetch()}
                    >
                      Retry
                    </button>
                  </div>
                </td>
              </tr>
            )}

            {/* Empty state */}
            {!isLoading && !isError && entries.length === 0 && (
              <tr>
                <td colSpan={sortableColumns.length}>
                  <div className={styles['emptyState']}>
                    <svg
                      className={styles['emptyIcon']}
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                      />
                    </svg>
                    <p className={styles['emptyMessage']}>
                      No audit log entries found matching the current filters.
                    </p>
                  </div>
                </td>
              </tr>
            )}

            {/* Data rows */}
            {!isLoading &&
              !isError &&
              entries.map((entry) => {
                const isExpanded = expandedRows.has(entry.id);
                return (
                  <RowGroup
                    key={entry.id}
                    entry={entry}
                    isExpanded={isExpanded}
                    onToggle={toggleRow}
                    colSpan={sortableColumns.length}
                  />
                );
              })}
          </tbody>
        </table>
      </div>

      {/* Load More */}
      {!isLoading && !isError && entries.length > 0 && (
        <div className={styles['loadMoreContainer']}>
          {totalElements !== undefined && (
            <span className={styles['loadMoreInfo']}>
              Showing {entries.length}
              {totalElements > 0 ? ` of ${totalElements}` : ''} entries
            </span>
          )}
          {hasNextPage && (
            <button
              type="button"
              className={styles['loadMoreBtn']}
              onClick={() => void fetchNextPage()}
              disabled={isFetchingNextPage}
            >
              {isFetchingNextPage ? 'Loading...' : 'Load More'}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
