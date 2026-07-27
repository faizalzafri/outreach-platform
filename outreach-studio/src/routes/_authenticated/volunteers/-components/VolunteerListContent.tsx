/**
 * Volunteer List Content (lazy-loaded)
 *
 * Displays volunteers in a DataTable with columns: employee ID, name, department,
 * location, total events, avg score, availability, and actions.
 * Includes a full-text search bar with 300ms debounce.
 */

import { useState, useMemo } from 'react';
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
  UNAVAILABLE: 'Unavailable',
  ON_LEAVE: 'On Leave',
};

function AvailabilityBadge({ availability }: { availability: VolunteerAvailability }) {
  const variant = availability.toLowerCase().replace(' ', '_');
  return (
    <span className={`${styles['availabilityBadge']} ${styles[`availabilityBadge--${variant}`]}`}>
      {AVAILABILITY_LABEL[availability] ?? availability}
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
    accessorKey: 'name',
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
    accessorKey: 'location',
    header: 'Location',
    enableSorting: true,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'totalEvents',
    header: 'Total Events',
    enableSorting: true,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'averageScore',
    header: 'Avg Score',
    enableSorting: true,
    enableColumnFilter: false,
    cell: ({ getValue }) => {
      const score = getValue() as number | null;
      return score !== null ? score.toFixed(1) : '—';
    },
  },
  {
    accessorKey: 'availability',
    header: 'Availability',
    enableSorting: true,
    enableColumnFilter: true,
    meta: {
      filterType: 'select',
      filterOptions: [
        { label: 'Available', value: 'AVAILABLE' },
        { label: 'Unavailable', value: 'UNAVAILABLE' },
        { label: 'On Leave', value: 'ON_LEAVE' },
      ],
    },
    cell: ({ getValue }) => (
      <AvailabilityBadge availability={getValue() as VolunteerAvailability} />
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
          placeholder="Search by name, employee ID, skills, or location..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          aria-label="Search volunteers"
        />
      </div>

      <DataTable<Volunteer>
        columns={columns}
        queryKey={queryKey}
        endpoint="/events/volunteers"
        defaultPageSize={search.size}
        emptyMessage="No volunteers found."
      />
    </div>
  );
}
