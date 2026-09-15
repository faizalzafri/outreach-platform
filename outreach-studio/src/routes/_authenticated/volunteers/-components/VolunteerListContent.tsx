/**
 * Volunteer List Content (lazy-loaded)
 *
 * Displays the volunteer directory from the /volunteers endpoint.
 * Includes a full-text search bar (searches skills, base location, and
 * department) with 300ms debounce.
 */

import { useMemo, useState } from 'react';
import { Link } from '@tanstack/react-router';
import type { ColumnDef } from '@tanstack/react-table';

import { DataTable } from '@/components/data-table/DataTable';
import { queryKeys } from '@/lib/query-keys';
import { useDebounce } from '@/hooks/useDebounce';
import type { Volunteer, VolunteerAvailability } from '@/types/domain';

import { Route } from '../index';
import styles from './VolunteerListContent.module.css';

// ---------------------------------------------------------------------------
// Availability badge
// ---------------------------------------------------------------------------

const AVAILABILITY_LABEL: Record<VolunteerAvailability, string> = {
  AVAILABLE: 'Available',
  BUSY: 'Busy',
  ON_LEAVE: 'On Leave',
};

function AvailabilityBadge({ availability }: { availability: VolunteerAvailability }) {
  const variant = availability.toLowerCase();
  return (
    <span className={`${styles['availabilityBadge']} ${styles[`availabilityBadge--${variant}`]}`}>
      {AVAILABILITY_LABEL[availability]}
    </span>
  );
}

// ---------------------------------------------------------------------------
// Column definitions
// ---------------------------------------------------------------------------

const columns: ColumnDef<Volunteer, unknown>[] = [
  {
    accessorKey: 'employeeId',
    header: 'Employee ID',
    enableSorting: true,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'fullName',
    header: 'Name',
    enableSorting: true,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'department',
    header: 'Department',
    enableSorting: true,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'availability',
    header: 'Availability',
    enableSorting: true,
    enableColumnFilter: false,
    cell: ({ row }) => <AvailabilityBadge availability={row.original.availability} />,
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
          params={{ employeeId: row.original.employeeId }}
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

  const endpoint = debouncedSearch
    ? `/volunteers?search=${encodeURIComponent(debouncedSearch)}`
    : '/volunteers';

  return (
    <div className={styles['container']}>
      <div className={styles['header']}>
        <h1 className={styles['pageTitle']}>Volunteers</h1>
      </div>

      {/* Full-text search bar */}
      <div className={styles['searchBar']}>
        <input
          type="text"
          className={styles['searchInput']}
          placeholder="Search by skills, location, or department..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          aria-label="Search volunteers"
        />
      </div>

      <DataTable<Volunteer>
        columns={columns}
        queryKey={queryKey}
        endpoint={endpoint}
        defaultPageSize={search.size}
        emptyMessage="No volunteers found."
      />
    </div>
  );
}
