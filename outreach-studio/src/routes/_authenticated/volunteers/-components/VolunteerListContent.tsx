/**
 * Volunteer List Content (lazy-loaded)
 *
 * Displays registered volunteers (users with POC role) from the /admin/users endpoint
 * filtered by role=POC. These are platform users who serve as Points of Contact for events.
 *
 * Includes a full-text search bar with 300ms debounce.
 */

import { useState, useMemo } from 'react';
import { Link } from '@tanstack/react-router';
import type { ColumnDef } from '@tanstack/react-table';

import { DataTable } from '@/components/data-table/DataTable';
import { queryKeys } from '@/lib/query-keys';
import { useDebounce } from '@/hooks/useDebounce';
import type { User } from '@/types/domain';

import { Route } from '../index';
import styles from './VolunteerListContent.module.css';

// ---------------------------------------------------------------------------
// Status badge
// ---------------------------------------------------------------------------

function StatusBadge({ enabled }: { enabled: boolean }) {
  const variant = enabled ? 'enabled' : 'disabled';
  const label = enabled ? 'Active' : 'Disabled';
  return (
    <span className={`${styles['availabilityBadge']} ${styles[`availabilityBadge--${variant}`]}`}>
      {label}
    </span>
  );
}

// ---------------------------------------------------------------------------
// Column definitions
// ---------------------------------------------------------------------------

const columns: ColumnDef<User, unknown>[] = [
  {
    accessorKey: 'username',
    header: 'Name',
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
    accessorKey: 'enabled',
    header: 'Status',
    enableSorting: true,
    enableColumnFilter: true,
    meta: {
      filterType: 'select',
      filterOptions: [
        { label: 'Active', value: 'true' },
        { label: 'Disabled', value: 'false' },
      ],
    },
    cell: ({ row }) => (
      <StatusBadge enabled={row.original.enabled} />
    ),
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
        Registered volunteers (POC users) who serve as Points of Contact for events.
      </p>

      {/* Full-text search bar */}
      <div className={styles['searchBar']}>
        <input
          type="text"
          className={styles['searchInput']}
          placeholder="Search by name or email..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          aria-label="Search volunteers"
        />
      </div>

      <DataTable<User>
        columns={columns}
        queryKey={queryKey}
        endpoint="/admin/users?role=POC"
        defaultPageSize={search.size}
        emptyMessage="No volunteers found."
      />
    </div>
  );
}
