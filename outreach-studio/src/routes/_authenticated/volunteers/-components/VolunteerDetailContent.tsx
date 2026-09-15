/**
 * Volunteer Detail Content (lazy-loaded)
 *
 * Displays volunteer profile: employee ID, name, email, department,
 * location, skills. Shows paginated participation history.
 * Provides availability status with optimistic update action, gated to
 * PMO/ADMIN/TENANT_ADMIN/PLATFORM_ADMIN — POC sees a read-only badge.
 */

import { useState, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useOptimisticMutation } from '@/hooks/useOptimisticMutation';
import { usePermission } from '@/hooks/usePermission';
import type { AttendanceStatus, Volunteer, VolunteerAvailability, VolunteerHistoryEntry } from '@/types/domain';
import type { PageResponse } from '@/types/api';

import { Route } from '../$employeeId';
import styles from './VolunteerDetailContent.module.css';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const AVAILABILITY_OPTIONS: VolunteerAvailability[] = ['AVAILABLE', 'BUSY', 'ON_LEAVE'];

const AVAILABILITY_LABEL: Record<VolunteerAvailability, string> = {
  AVAILABLE: 'Available',
  BUSY: 'Busy',
  ON_LEAVE: 'On Leave',
};

const ATTENDANCE_LABEL: Record<AttendanceStatus, string> = {
  REGISTERED: 'Registered',
  ATTENDED: 'Attended',
  NOT_ATTENDED: 'Not Attended',
  UNREGISTERED: 'Unregistered',
};

const HISTORY_PAGE_SIZE = 5;

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function VolunteerDetailContent() {
  const { employeeId } = Route.useParams();
  const queryClient = useQueryClient();
  const { hasPermission: canManage } = usePermission([
    'ROLE_PMO',
    'ROLE_ADMIN',
    'ROLE_TENANT_ADMIN',
    'ROLE_PLATFORM_ADMIN',
  ]);

  const [optimisticAvailability, setOptimisticAvailability] = useState<VolunteerAvailability | null>(null);
  const [availabilityError, setAvailabilityError] = useState<string | null>(null);
  const [historyPage, setHistoryPage] = useState(0);

  // Fetch volunteer detail
  const { data: volunteer, isLoading, isError, error, refetch } = useQuery<Volunteer>({
    queryKey: queryKeys.volunteers.detail(employeeId),
    queryFn: async () => {
      const response = await httpClient.get<Volunteer>(`/volunteers/${employeeId}`);
      return response.data;
    },
  });

  // Fetch participation history (paginated, 0-indexed to match Spring's Pageable)
  const { data: historyData } = useQuery<PageResponse<VolunteerHistoryEntry>>({
    queryKey: [...queryKeys.volunteers.detail(employeeId), 'history', historyPage],
    queryFn: async () => {
      const response = await httpClient.get<PageResponse<VolunteerHistoryEntry>>(
        `/volunteers/${employeeId}/history`,
        { params: { page: historyPage, size: HISTORY_PAGE_SIZE } },
      );
      return response.data;
    },
    enabled: !!volunteer,
  });

  // Availability mutation with optimistic update and rollback on server rejection
  const availabilityMutation = useOptimisticMutation<Volunteer, VolunteerAvailability>({
    mutationFn: async (newAvailability) => {
      const response = await httpClient.put<Volunteer>(
        `/volunteers/${employeeId}`,
        { availability: newAvailability },
      );
      return response.data;
    },
    queryKey: queryKeys.volunteers.detail(employeeId),
    optimisticUpdate: (cached, newAvailability) => {
      if (!cached) return cached;
      setOptimisticAvailability(newAvailability);
      setAvailabilityError(null);
      return { ...cached, availability: newAvailability };
    },
    rollbackTimeout: 1000,
    onSuccess: () => {
      setOptimisticAvailability(null);
      void queryClient.invalidateQueries({ queryKey: queryKeys.volunteers.lists() });
    },
    onError: (message) => {
      setOptimisticAvailability(null);
      setAvailabilityError(message);
    },
    invalidateKeys: [queryKeys.volunteers.lists()],
  });

  const handleAvailabilityChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      const newValue = e.target.value as VolunteerAvailability;
      availabilityMutation.mutate(newValue);
    },
    [availabilityMutation],
  );

  // Loading state
  if (isLoading) {
    return (
      <div className={styles['container']}>
        <div className={styles['skeleton']} style={{ height: '2rem', width: '200px' }} />
        <div className={styles['skeletonGrid']}>
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className={styles['skeleton']} style={{ height: '1.5rem' }} />
          ))}
        </div>
      </div>
    );
  }

  // Error state
  if (isError || !volunteer) {
    return (
      <div className={styles['container']}>
        <div className={styles['errorState']}>
          <p className={styles['errorMessage']}>
            {(error as { message?: string })?.message ?? 'Failed to load volunteer'}
          </p>
          <button type="button" className={styles['retryBtn']} onClick={() => void refetch()}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  const displayAvailability = optimisticAvailability ?? volunteer.availability;
  const skills = volunteer.skills
    ? volunteer.skills.split(',').map((s) => s.trim()).filter(Boolean)
    : [];

  return (
    <div className={styles['container']}>
      {/* Header */}
      <div className={styles['header']}>
        <div>
          <h1 className={styles['pageTitle']}>{volunteer.fullName}</h1>
          <p className={styles['employeeId']}>{volunteer.employeeId}</p>
        </div>
      </div>

      {/* Profile */}
      <dl className={styles['profileGrid']}>
        <div className={styles['profileField']}>
          <dt className={styles['profileLabel']}>Employee ID</dt>
          <dd className={styles['profileValue']}>{volunteer.employeeId}</dd>
        </div>
        <div className={styles['profileField']}>
          <dt className={styles['profileLabel']}>Name</dt>
          <dd className={styles['profileValue']}>{volunteer.fullName}</dd>
        </div>
        <div className={styles['profileField']}>
          <dt className={styles['profileLabel']}>Email</dt>
          <dd className={styles['profileValue']}>{volunteer.email}</dd>
        </div>
        <div className={styles['profileField']}>
          <dt className={styles['profileLabel']}>Department</dt>
          <dd className={styles['profileValue']}>{volunteer.department}</dd>
        </div>
        <div className={styles['profileField']}>
          <dt className={styles['profileLabel']}>Location</dt>
          <dd className={styles['profileValue']}>{volunteer.baseLocation}</dd>
        </div>
        <div className={styles['profileField']}>
          <dt className={styles['profileLabel']}>Designation</dt>
          <dd className={styles['profileValue']}>{volunteer.designation}</dd>
        </div>
        <div className={styles['profileField']}>
          <dt className={styles['profileLabel']}>Skills</dt>
          <dd className={styles['profileValue']}>
            {skills.length > 0 ? (
              <ul className={styles['skillsList']}>
                {skills.map((skill) => (
                  <li key={skill} className={styles['skillTag']}>{skill}</li>
                ))}
              </ul>
            ) : (
              '—'
            )}
          </dd>
        </div>
        <div className={styles['profileField']}>
          <dt className={styles['profileLabel']}>Total Events</dt>
          <dd className={styles['profileValue']}>{volunteer.totalEventsParticipated}</dd>
        </div>
        <div className={styles['profileField']}>
          <dt className={styles['profileLabel']}>Avg. Feedback Score</dt>
          <dd className={styles['profileValue']}>
            {volunteer.avgFeedbackScore !== null ? volunteer.avgFeedbackScore.toFixed(1) : '—'}
          </dd>
        </div>
      </dl>

      {/* Availability */}
      <div className={styles['availabilitySection']}>
        <span
          className={`${styles['availabilityBadge']} ${styles[`availabilityBadge--${displayAvailability.toLowerCase()}`]} ${optimisticAvailability ? styles['availabilityBadge--pending'] : ''}`}
        >
          {AVAILABILITY_LABEL[displayAvailability]}
        </span>
        {canManage ? (
          <select
            className={styles['availabilitySelect']}
            value={displayAvailability}
            onChange={handleAvailabilityChange}
            disabled={availabilityMutation.isPending}
            aria-label="Update availability"
          >
            {AVAILABILITY_OPTIONS.map((opt) => (
              <option key={opt} value={opt}>{AVAILABILITY_LABEL[opt]}</option>
            ))}
          </select>
        ) : null}
        {availabilityError && (
          <span className={styles['availabilityError']} role="alert">{availabilityError}</span>
        )}
      </div>

      {/* Participation History */}
      <div className={styles['historySection']}>
        <h2 className={styles['sectionTitle']}>Participation History</h2>
        {historyData && historyData.content.length > 0 ? (
          <>
            <table className={styles['historyTable']}>
              <thead>
                <tr>
                  <th scope="col">Event</th>
                  <th scope="col">Date</th>
                  <th scope="col">City</th>
                  <th scope="col">Attendance</th>
                </tr>
              </thead>
              <tbody>
                {historyData.content.map((record) => (
                  <tr key={record.eventId}>
                    <td>{record.eventName}</td>
                    <td>{new Date(record.eventDate).toLocaleDateString()}</td>
                    <td>{record.city}</td>
                    <td>{ATTENDANCE_LABEL[record.attendanceStatus]}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className={styles['paginationControls']}>
              <button
                type="button"
                className={styles['paginationBtn']}
                disabled={historyPage === 0}
                onClick={() => setHistoryPage((p) => p - 1)}
              >
                Previous
              </button>
              <span className={styles['paginationInfo']}>
                Page {historyPage + 1} of {historyData.totalPages}
              </span>
              <button
                type="button"
                className={styles['paginationBtn']}
                disabled={historyPage + 1 >= historyData.totalPages}
                onClick={() => setHistoryPage((p) => p + 1)}
              >
                Next
              </button>
            </div>
          </>
        ) : (
          <p className={styles['emptyState']}>No participation history yet.</p>
        )}
      </div>
    </div>
  );
}
