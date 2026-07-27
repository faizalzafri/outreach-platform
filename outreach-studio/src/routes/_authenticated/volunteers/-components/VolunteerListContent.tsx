/**
 * Volunteer List Content (lazy-loaded)
 *
 * Displays platform users (filtered by ROLE_POC) from the /admin/users endpoint.
 * The backend has no standalone /events/volunteers list — volunteers are only
 * accessible per-event via /events/{eventId}/volunteers.
 *
 * Includes a full-text search bar with 300ms debounce.
 */

import { useState, useMemo } from 'react';
import { Link } from '@tanstack/react-router';
import type { ColumnDef } from '@tanstack/react-table';

import { DataTable } from '@/components/data-table/DataTable';
import { queryKeys } from '@/lib/query-keys';
import { useDebounce } from '@/hooks/useDebounce';
import type { User, UserStatus } from '@/types/domain';

import { Route } from '../index';
import styles from './VolunteerListContent.module.css';

// ---------------------------------------------------------------------------
// Status badge
// ---------------------------------------------------------------------------

const STATUS_LABEL: Record<UserStatus, string> = {
  ENABLED: 'Active',
  DISABLED: 'Disabled',
  LOCKED: 'Locked',
};

function StatusBadge({ status }: { status: UserStatus }) {
  const variant = status.toLowerCase();
  return (
    <span className={`${styles['availabilityBadge']} ${styles[`availabilityBadge--${variant}`]}`}>
      {STATUS_LABEL[status] ?? status}
    </span>
  );
}

// ---------------------------------------------------------------------------
// Column definitions
// ---------------------------------------------------------------------------

const columns: ColumnDef<User, unknown>[] = [
  {
    accessorKey: 'username',
    header: 'Username',
    enableSorting: true,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'email',
    header: 'Email',
    enableSorting: true,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'role',
    header: 'Role',
    enableSorting: true,
    enableColumnFilter: false,
    cell: ({ getValue }) => {
      const role = getValue() as string;
      return role?.replace('ROLE_', '') ?? '—';
    },
  },
  {
    accessorKey: 'status',
    header: 'Status',
    enableSorting: true,
    enableColumnFilter: true,
    meta: {
      filterType: 'select',
      filterOptions: [
        { label: 'Active', value: 'ENABLED' },
        { label: 'Disabled', value: 'DISABLED' },
        { label: 'Locked', value: 'LOCKED' },
      ],
    },
    cell: ({ getValue }) => (
      <StatusBadge status={getValue() as UserStatus} />
    ),
  },
  {
    accessorKey: 'lastLogin',
    header: 'Last Login',
    enableSorting: true,
    enableColumnFilter: false,
    cell: ({ getValue }) => {
      const val = getValue() as string | null;
      return val ? new Date(val).toLocaleDateString() : '—';
    },
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
          to="/volunteers/$employeeId"
          params={{ employeeId: row.original.id }}
          className={styles['actionLink']}
        >
          View
        </Link>
      </div>
    ),
  },
];

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function VolunteerListContent() {
  const search = Route.useSearch();
  const [searchText, setSearchText] = useState(search.search ?? '');
  const debouncedSearch = useDebounce(searchText, 300);

  const queryKey = useMemo(
    () => queryKeys.volunteers.list({
      page: search.page,
      size: search.size,
      search: debouncedSearch || undefined,
    }),
    [search.page, search.size, debouncedSearch],
  );

  return (
    <div className={styles['container']}>
      <div className={styles['header']}>
        <h1 className={styles['pageTitle']}>Volunteers</h1>
      </div>

      {/* Info note about volunteer context */}
      <p className={styles['infoNote'] ?? ''} style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '1rem' }}>
        Select an event to view its enrolled volunteers, or browse all platform users below.
      </p>

      {/* Full-text search bar */}
      <div className={styles['searchBar']}>
        <input
          type="text"
          className={styles['searchInput']}
          placeholder="Search by username or email..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          aria-label="Search users"
        />
      </div>

      <DataTable<User>
        columns={columns}
        queryKey={queryKey}
        endpoint="/admin/users"
        defaultPageSize={search.size}
        emptyMessage="No users found."
      />
    </div>
  );
}
