/**
 * Generic DataTable component built on TanStack Table (headless) with
 * server-side pagination, sorting, and filtering via TanStack Query.
 *
 * Accepts column definitions, a query key, and an API endpoint,
 * making it reusable across all list views in the application.
 *
 * When a dataset exceeds 100 visible rows, the table switches to
 * virtualized rendering via @tanstack/react-virtual to maintain
 * >30fps scroll performance with large datasets.
 */

import { useState, useMemo, useCallback, useRef, useEffect, memo } from 'react';
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  type ColumnDef,
  type SortingState,
  type ColumnFiltersState,
  type VisibilityState,
  type PaginationState,
  type Row,
  type Cell,
} from '@tanstack/react-table';
import { useQuery, type QueryKey } from '@tanstack/react-query';
import { useVirtualizer } from '@tanstack/react-virtual';

import { httpClient } from '@/lib/http-client';
import { useDebounce } from '@/hooks/useDebounce';
import type { PageResponse } from '@/types/api';

import styles from './DataTable.module.css';

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

export interface DataTableProps<TData> {
  columns: ColumnDef<TData, unknown>[];
  queryKey: QueryKey;
  endpoint: string;
  defaultPageSize?: number;
  searchPlaceholder?: string;
  enableColumnVisibility?: boolean;
  emptyMessage?: string;
  /** Accessible caption describing the table contents */
  caption?: string;
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const PAGE_SIZE_OPTIONS = [10, 25, 50, 100] as const;

/**
 * Row count threshold above which virtualization is enabled.
 * Below this count the table renders normally without virtualization overhead.
 */
const VIRTUALIZATION_THRESHOLD = 100;

/** Estimated height of each table row in pixels (used by the virtualizer). */
const ROW_HEIGHT_ESTIMATE = 44;

// ---------------------------------------------------------------------------
// Memoized row component — applied when processing >100 items to reduce
// re-renders during scroll. Only re-renders when its own row data changes.
// ---------------------------------------------------------------------------

interface VirtualRowProps<TData> {
  row: Row<TData>;
  cells: Cell<TData, unknown>[];
}

const VirtualRowInner = memo(function VirtualRowInner<TData>({
  row: _row,
  cells,
}: VirtualRowProps<TData>) {
  return (
    <>
      {cells.map((cell) => (
        <td key={cell.id} className={styles['td']}>
          {flexRender(cell.column.columnDef.cell, cell.getContext())}
        </td>
      ))}
    </>
  );
}) as <TData>(props: VirtualRowProps<TData>) => React.JSX.Element;

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function DataTable<TData>({
  columns,
  queryKey,
  endpoint,
  defaultPageSize = 10,
  searchPlaceholder: _searchPlaceholder,
  enableColumnVisibility = true,
  emptyMessage = 'No records found.',
  caption,
}: DataTableProps<TData>) {
  // --- Table state ---
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize: defaultPageSize,
  });
  const [sorting, setSorting] = useState<SortingState>([]);
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({});
  const [visibilityOpen, setVisibilityOpen] = useState(false);

  // Debounce column filters by 300ms
  const debouncedFilters = useDebounce(columnFilters, 300);

  // --- Build query params ---
  const queryParams = useMemo(() => {
    const params: Record<string, string | number> = {
      page: pagination.pageIndex, // 0-based for Spring Boot Pageable
      size: pagination.pageSize,
    };

    // Sorting — Spring Boot Pageable format: "fieldName,direction"
    if (sorting.length > 0) {
      const sort = sorting[0];
      if (sort) {
        params['sort'] = `${sort.id},${sort.desc ? 'desc' : 'asc'}`;
      }
    }

    // Column filters
    for (const filter of debouncedFilters) {
      if (filter.value !== undefined && filter.value !== null && filter.value !== '') {
        params[filter.id] = filter.value as string;
      }
    }

    return params;
  }, [pagination, sorting, debouncedFilters]);

  // --- Data fetching ---
  const { data, isLoading, isError, error, refetch } = useQuery<PageResponse<TData>>({
    queryKey: [...queryKey, queryParams],
    queryFn: async () => {
      const response = await httpClient.get<PageResponse<TData>>(endpoint, {
        params: queryParams,
      });
      return response.data;
    },
  });

  // --- TanStack Table instance ---
  const table = useReactTable<TData>({
    data: data?.content ?? [],
    columns,
    pageCount: data?.totalPages ?? -1,
    state: {
      pagination,
      sorting,
      columnFilters,
      columnVisibility,
    },
    onPaginationChange: setPagination,
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    onColumnVisibilityChange: setColumnVisibility,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    manualSorting: true,
    manualFiltering: true,
  });

  // --- Virtualization: only engage when row count exceeds threshold ---
  const rows = table.getRowModel().rows;
  const shouldVirtualize = rows.length > VIRTUALIZATION_THRESHOLD;

  const tableContainerRef = useRef<HTMLDivElement>(null);

  const rowVirtualizer = useVirtualizer({
    count: rows.length,
    getScrollElement: () => tableContainerRef.current,
    estimateSize: () => ROW_HEIGHT_ESTIMATE,
    overscan: 10,
    enabled: shouldVirtualize,
  });

  // --- Column visibility: ensure at least one column always visible ---
  const handleColumnVisibilityChange = useCallback(
    (columnId: string, isVisible: boolean) => {
      if (!isVisible) {
        // Count currently visible columns
        const visibleCount = table
          .getAllColumns()
          .filter((col) => col.getIsVisible()).length;

        // Block hiding if this is the last visible column
        if (visibleCount <= 1) return;
      }

      setColumnVisibility((prev) => ({
        ...prev,
        [columnId]: isVisible,
      }));
    },
    [table],
  );

  // --- Column visibility dropdown outside-click close ---
  const visibilityRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!visibilityOpen) return;

    function handleClickOutside(event: MouseEvent) {
      if (
        visibilityRef.current &&
        !visibilityRef.current.contains(event.target as Node)
      ) {
        setVisibilityOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [visibilityOpen]);

  // --- Pagination helpers ---
  const totalElements = data?.totalElements ?? 0;
  const currentPage = pagination.pageIndex + 1; // display as 1-based
  const totalPages = data?.totalPages ?? 0;
  const canPreviousPage = pagination.pageIndex > 0;
  const canNextPage = currentPage < totalPages;

  // --- Render: Filters ---
  const filterableColumns = table.getAllColumns().filter((col) => col.getCanFilter());

  // --- Render ---
  return (
    <div className={styles['container']}>
      {/* Toolbar */}
      {(filterableColumns.length > 0 || enableColumnVisibility) && (
        <div className={styles['toolbar']}>
          {/* Column filters */}
          <div className={styles['filters']}>
            {filterableColumns.map((column) => {
              const filterMeta = column.columnDef.meta as
                | { filterType?: 'text' | 'select'; filterOptions?: { label: string; value: string }[] }
                | undefined;
              const filterType = filterMeta?.filterType ?? 'text';
              const filterValue = (column.getFilterValue() as string) ?? '';

              if (filterType === 'select' && filterMeta?.filterOptions) {
                return (
                  <select
                    key={column.id}
                    className={styles['filterSelect']}
                    value={filterValue}
                    onChange={(e) => column.setFilterValue(e.target.value || undefined)}
                    aria-label={`Filter ${column.id}`}
                  >
                    <option value="">All</option>
                    {filterMeta.filterOptions.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                );
              }

              return (
                <input
                  key={column.id}
                  type="text"
                  className={styles['filterInput']}
                  placeholder={`Filter ${column.id}...`}
                  value={filterValue}
                  onChange={(e) => column.setFilterValue(e.target.value || undefined)}
                  aria-label={`Filter ${column.id}`}
                />
              );
            })}
          </div>

          {/* Column visibility dropdown */}
          {enableColumnVisibility && (
            <div className={styles['columnVisibility']} ref={visibilityRef}>
              <button
                type="button"
                className={styles['columnVisibilityBtn']}
                onClick={() => setVisibilityOpen((prev) => !prev)}
                aria-expanded={visibilityOpen}
                aria-haspopup="true"
              >
                Columns
              </button>
              {visibilityOpen && (
                <div
                  className={styles['columnVisibilityDropdown']}
                  role="menu"
                >
                  {table.getAllColumns().map((column) => {
                    if (!column.getCanHide()) return null;
                    const isVisible = column.getIsVisible();
                    const visibleCount = table
                      .getAllColumns()
                      .filter((c) => c.getIsVisible()).length;
                    const isLastVisible = isVisible && visibleCount <= 1;

                    return (
                      <label
                        key={column.id}
                        className={styles['columnVisibilityItem']}
                        role="menuitemcheckbox"
                        aria-checked={isVisible}
                      >
                        <input
                          type="checkbox"
                          className={styles['columnVisibilityCheckbox']}
                          checked={isVisible}
                          disabled={isLastVisible}
                          onChange={(e) =>
                            handleColumnVisibilityChange(column.id, e.target.checked)
                          }
                        />
                        {typeof column.columnDef.header === 'string'
                          ? column.columnDef.header
                          : column.id}
                      </label>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Table — uses a scrollable container with virtualization for large datasets */}
      <div
        ref={tableContainerRef}
        className={`${styles['tableWrapper']} ${shouldVirtualize ? styles['tableWrapperVirtual'] : ''}`}
      >
        <table className={styles['table']}>
          {caption && <caption className="sr-only">{caption}</caption>}
          <thead className={styles['thead']}>
            {table.getHeaderGroups().map((headerGroup) => (
              <tr key={headerGroup.id}>
                {headerGroup.headers.map((header) => {
                  const canSort = header.column.getCanSort();
                  const sorted = header.column.getIsSorted();

                  return (
                    <th
                      key={header.id}
                      className={`${styles['th']} ${canSort ? styles['thSortable'] : ''}`}
                      onClick={canSort ? header.column.getToggleSortingHandler() : undefined}
                      aria-sort={
                        sorted === 'asc'
                          ? 'ascending'
                          : sorted === 'desc'
                            ? 'descending'
                            : canSort
                              ? 'none'
                              : undefined
                      }
                      scope="col"
                    >
                      <span className={styles['thContent']}>
                        {header.isPlaceholder
                          ? null
                          : flexRender(header.column.columnDef.header, header.getContext())}
                        {canSort && (
                          <span
                            className={`${styles['sortIndicator']} ${sorted ? styles['sortIndicatorActive'] : ''}`}
                            aria-hidden="true"
                          >
                            {sorted === 'asc' ? '↑' : sorted === 'desc' ? '↓' : '↕'}
                          </span>
                        )}
                      </span>
                    </th>
                  );
                })}
              </tr>
            ))}
          </thead>

          <tbody className={styles['tbody']}>
            {/* Loading skeleton */}
            {isLoading && (
              <>
                {Array.from({ length: pagination.pageSize }, (_, i) => (
                  <tr key={`skeleton-${String(i)}`} className={styles['skeletonRow']}>
                    {table.getVisibleFlatColumns().map((col) => (
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
                <td
                  colSpan={table.getVisibleFlatColumns().length}
                  className={styles['td']}
                >
                  <div className={styles['errorState']}>
                    <p className={styles['errorMessage']}>
                      {(error as { message?: string })?.message ?? 'Failed to load data.'}
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
            {!isLoading && !isError && data?.content.length === 0 && (
              <tr>
                <td
                  colSpan={table.getVisibleFlatColumns().length}
                  className={styles['td']}
                >
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
                        d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
                      />
                    </svg>
                    <p className={styles['emptyMessage']}>{emptyMessage}</p>
                  </div>
                </td>
              </tr>
            )}

            {/* Data rows — standard rendering for small datasets */}
            {!isLoading && !isError && !shouldVirtualize &&
              rows.map((row) => (
                <tr key={row.id} className={styles['tr']}>
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id} className={styles['td']}>
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              ))}

            {/* Virtualized rows — for datasets exceeding 100 rows */}
            {!isLoading && !isError && shouldVirtualize && (() => {
              const virtualItems = rowVirtualizer.getVirtualItems();
              return (
                <>
                  {/* Top spacer to position visible rows correctly in the scroll area */}
                  {virtualItems.length > 0 && (
                    <tr
                      className={styles['virtualSpacer']}
                      aria-hidden="true"
                    >
                      <td
                        colSpan={table.getVisibleFlatColumns().length}
                        style={{ height: `${virtualItems[0]?.start ?? 0}px` }}
                      />
                    </tr>
                  )}

                  {virtualItems.map((virtualRow) => {
                    const row = rows[virtualRow.index]!;
                    return (
                      <tr
                        key={row.id}
                        className={styles['tr']}
                        data-index={virtualRow.index}
                        ref={rowVirtualizer.measureElement}
                      >
                        <VirtualRowInner row={row} cells={row.getVisibleCells()} />
                      </tr>
                    );
                  })}

                  {/* Bottom spacer */}
                  {virtualItems.length > 0 && (
                    <tr
                      className={styles['virtualSpacer']}
                      aria-hidden="true"
                    >
                      <td
                        colSpan={table.getVisibleFlatColumns().length}
                        style={{
                          height: `${rowVirtualizer.getTotalSize() - (virtualItems[virtualItems.length - 1]?.end ?? 0)}px`,
                        }}
                      />
                    </tr>
                  )}
                </>
              );
            })()}
          </tbody>
        </table>
      </div>

      {/* Pagination footer */}
      <div className={styles['pagination']}>
        <span className={styles['paginationInfo']}>
          {totalElements} total record{totalElements !== 1 ? 's' : ''} — Page{' '}
          {totalPages > 0 ? currentPage : 0} of {totalPages}
        </span>

        <div className={styles['paginationControls']}>
          {/* Page size selector */}
          <select
            className={styles['pageSizeSelect']}
            value={pagination.pageSize}
            onChange={(e) =>
              setPagination({ pageIndex: 0, pageSize: Number(e.target.value) })
            }
            aria-label="Page size"
          >
            {PAGE_SIZE_OPTIONS.map((size) => (
              <option key={size} value={size}>
                {size} / page
              </option>
            ))}
          </select>

          {/* Navigation buttons */}
          <button
            type="button"
            className={styles['paginationBtn']}
            onClick={() => setPagination((prev) => ({ ...prev, pageIndex: 0 }))}
            disabled={!canPreviousPage}
            aria-label="First page"
          >
            «
          </button>
          <button
            type="button"
            className={styles['paginationBtn']}
            onClick={() =>
              setPagination((prev) => ({ ...prev, pageIndex: prev.pageIndex - 1 }))
            }
            disabled={!canPreviousPage}
            aria-label="Previous page"
          >
            ‹
          </button>
          <button
            type="button"
            className={styles['paginationBtn']}
            onClick={() =>
              setPagination((prev) => ({ ...prev, pageIndex: prev.pageIndex + 1 }))
            }
            disabled={!canNextPage}
            aria-label="Next page"
          >
            ›
          </button>
          <button
            type="button"
            className={styles['paginationBtn']}
            onClick={() =>
              setPagination((prev) => ({
                ...prev,
                pageIndex: totalPages - 1,
              }))
            }
            disabled={!canNextPage}
            aria-label="Last page"
          >
            »
          </button>
        </div>
      </div>
    </div>
  );
}
