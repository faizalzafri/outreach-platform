/**
 * Event List Content (lazy-loaded)
 *
 * Displays events in a DataTable with columns: code, name, status, date,
 * city, POC, volunteer count, and actions (View/Edit/Delete).
 * Includes a "Create Event" button navigating to the create form.
 */

import { useState, useMemo } from 'react';
import { Link } from '@tanstack/react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { ColumnDef } from '@tanstack/react-table';

import { DataTable } from '@/components/data-table/DataTable';
import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useDebounce } from '@/hooks/useDebounce';
import { useToast } from '@/hooks/useToast';
import { usePermission } from '@/hooks/usePermission';
import type { NormalizedError } from '@/types/api';
import type { Event, EventStatus } from '@/types/domain';

import { Route } from '../index';
import styles from './EventListContent.module.css';

// ---------------------------------------------------------------------------
// Status badge styling
// ---------------------------------------------------------------------------

const STATUS_VARIANT: Record<EventStatus, string> = {
  DRAFT: 'draft',
  PUBLISHED: 'published',
  ACTIVE: 'active',
  COMPLETED: 'completed',
  ARCHIVED: 'archived',
  CANCELLED: 'cancelled',
};

function StatusBadge({ status }: { status: EventStatus }) {
  const variant = STATUS_VARIANT[status] ?? 'draft';
  return (
    <span className={`${styles['badge']} ${styles[`badge--${variant}`]}`}>
      {status}
    </span>
  );
}

// ---------------------------------------------------------------------------
// Column definitions
// ---------------------------------------------------------------------------

function buildColumns(
  onDelete: (event: Event) => void,
  canManage: boolean,
): ColumnDef<Event, unknown>[] {
  return [
  {
    accessorKey: 'eventCode',
    header: 'Code',
    enableSorting: true,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'eventName',
    header: 'Name',
    enableSorting: true,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'status',
    header: 'Status',
    enableSorting: true,
    enableColumnFilter: true,
    meta: {
      filterType: 'select',
      filterOptions: [
        { label: 'Draft', value: 'DRAFT' },
        { label: 'Published', value: 'PUBLISHED' },
        { label: 'Active', value: 'ACTIVE' },
        { label: 'Completed', value: 'COMPLETED' },
        { label: 'Archived', value: 'ARCHIVED' },
        { label: 'Cancelled', value: 'CANCELLED' },
      ],
    },
    cell: ({ getValue }) => <StatusBadge status={getValue() as EventStatus} />,
  },
  {
    accessorKey: 'eventDate',
    header: 'Date',
    enableSorting: true,
    enableColumnFilter: false,
    cell: ({ row }) => {
      const start = row.original.eventDate;
      const end = row.original.eventEndDate;
      const fmt = (dateStr: string) =>
        new Date(dateStr + 'T00:00:00').toLocaleDateString();
      return (
        <span>
          {fmt(start)} – {fmt(end)}
        </span>
      );
    },
  },
  {
    accessorKey: 'city',
    header: 'City',
    enableSorting: true,
    enableColumnFilter: true,
    meta: { filterType: 'text' },
  },
  {
    accessorKey: 'createdBy',
    header: 'Created By',
    enableSorting: false,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'registeredCount',
    header: 'Volunteers',
    enableSorting: true,
    enableColumnFilter: false,
  },
  {
    id: 'actions',
    header: 'Actions',
    enableSorting: false,
    enableColumnFilter: false,
    enableHiding: false,
    cell: ({ row }) => (
      <div className={styles['actions']}>
        <Link
          to="/events/$eventId"
          params={{ eventId: row.original.id }}
          search={{ tab: 'overview' }}
          className={styles['actionLink']}
        >
          View
        </Link>
        {canManage && (
          <button
            type="button"
            className={styles['actionBtn']}
            aria-label={`Delete event ${row.original.eventName}`}
            onClick={() => onDelete(row.original)}
          >
            Delete
          </button>
        )}
      </div>
    ),
  },
  ];
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function EventListContent() {
  const search = Route.useSearch();
  const [searchText, setSearchText] = useState(search.search ?? '');
  const debouncedSearch = useDebounce(searchText, 300);
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();
  const { hasPermission: canManage } = usePermission([
    'ROLE_PMO',
    'ROLE_ADMIN',
    'ROLE_TENANT_ADMIN',
    'ROLE_PLATFORM_ADMIN',
  ]);

  const queryKey = useMemo(
    () => queryKeys.events.list({
      page: search.page,
      size: search.size,
      status: search.status,
      search: debouncedSearch || undefined,
    }),
    [search.page, search.size, search.status, debouncedSearch],
  );

  const deleteMutation = useMutation({
    mutationFn: async (eventId: string) => {
      await httpClient.delete(`/events/${eventId}`);
    },
    onSuccess: () => {
      toastSuccess('Event deleted');
      void queryClient.invalidateQueries({ queryKey: queryKeys.events.all });
    },
    onError: (error: NormalizedError) => {
      toastError(error.message || 'Failed to delete event');
    },
  });

  const columns = useMemo(
    () => buildColumns((event) => {
      if (window.confirm(`Delete event "${event.eventName}"? This cannot be undone.`)) {
        deleteMutation.mutate(event.id);
      }
    }, canManage),
    [deleteMutation, canManage],
  );

  return (
    <div className={styles['container']}>
      <div className={styles['header']}>
        <h1 className={styles['pageTitle']}>Events</h1>
        {canManage && (
          <Link to="/events/create" className={styles['createBtn']}>
            Create Event
          </Link>
        )}
      </div>

      {/* Search text filter */}
      <div className={styles['searchBar']}>
        <input
          type="text"
          className={styles['searchInput']}
          placeholder="Search events by name, code, or city..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          aria-label="Search events"
        />
      </div>

      <DataTable<Event>
        columns={columns}
        queryKey={queryKey}
        endpoint="/events"
        defaultPageSize={search.size}
        emptyMessage="No events found."
      />
    </div>
  );
}
