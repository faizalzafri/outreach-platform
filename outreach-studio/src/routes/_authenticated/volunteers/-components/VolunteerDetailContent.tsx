/**
 * Volunteer Detail Content (lazy-loaded)
 *
 * Displays volunteer profile: employee ID, name, email, department, location, skills, join date.
 * Shows paginated participation history and feedback score trend chart (recharts).
 * Provides availability status with optimistic update action.
 */

import { useState, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useOptimisticMutation } from '@/hooks/useOptimisticMutation';
import type { Volunteer, VolunteerAvailability } from '@/types/domain';
import type { PageResponse } from '@/types/api';

import { Route } from '../$employeeId';
import styles from './VolunteerDetailContent.module.css';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface ParticipationRecord {
  eventId: string;
  eventName: string;
  date: string;
  role: string;
  feedbackScore: number | null;
}

interface ScoreTrendPoint {
  date: string;
  score: number;
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const AVAILABILITY_OPTIONS: VolunteerAvailability[] = ['AVAILABLE', 'UNAVAILABLE', 'ON_LEAVE'];

const AVAILABILITY_LABEL: Record<VolunteerAvailability, string> = {
  AVAILABLE: 'Available',
  UNAVAILABLE: 'Unavailable',
  ON_LEAVE: 'On Leave',
};

const HISTORY_PAGE_SIZE = 5;

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function VolunteerDetailContent() {
  const { employeeId } = Route.useParams();
  const queryClient = useQueryClient();

  const [optimisticAvailability, setOptimisticAvailability] = useState<VolunteerAvailability | null>(null);
  const [availabilityError, setAvailabilityError] = useState<string | null>(null);
  const [historyPage, setHistoryPage] = useState(0);

  // Fetch volunteer detail
  const { data: volunteer, isLoading, isError, error, refetch } = useQuery<Volunteer>({
    queryKey: queryKeys.volunteers.detail(employeeId),
    queryFn: async () => {
      const response = await httpClient.get<Volunteer>(`/events/volunteers/${employeeId}`);
      return response.data;
    },
  });

  // Fetch participation history (paginated)
  const { data: historyData } = useQuery<PageResponse<ParticipationRecord>>({
    queryKey: [...queryKeys.volunteers.detail(employeeId), 'history', historyPage],
    queryFn: async () => {
      const response = await httpClient.get<PageResponse<ParticipationRecord>>(
        `/events/volunteers/${employeeId}/history`,
        { params: { page: historyPage + 1, size: HISTORY_PAGE_SIZE } },
      );
      return response.data;
    },
    enabled: !!volunteer,
  });

  // Fetch feedback score trend
  const { data: scoreTrend } = useQuery<ScoreTrendPoint[]>({
    queryKey: [...queryKeys.volunteers.detail(employeeId), 'score-trend'],
    queryFn: async () => {
      const response = await httpClient.get<ScoreTrendPoint[]>(
        `/events/volunteers/${employeeId}/score-trend`,
      );
      return response.data;
    },
    enabled: !!volunteer,
  });

  // Availability mutation with optimistic update and rollback on server rejection
  const availabilityMutation = useOptimisticMutation<Volunteer, VolunteerAvailability>({
    mutationFn: async (newAvailability) => {
      const response = await httpClient.put<Volunteer>(
        `/events/volunteers/${employeeId}/availability`,
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

  return (
    <div className={styles['container']}>
      {/* Header */}
      <div className={styles['header']}>
        <div>
          <h1 className={styles['pageTitle']}>{volunteer.name}</h1>
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
          <dd className={styles['profileValue']}>{volunteer.name}</dd>
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
          <dd className={styles['profileValue']}>{volunteer.location}</dd>
        </div>
        <div className={styles['profileField']}>
          <dt className={styles['profileLabel']}>Join Date</dt>
          <dd className={styles['profileValue']}>{new Date(volunteer.joinDate).toLocaleDateString()}</dd>
        </div>
        <div className={styles['profileField']}>
          <dt className={styles['profileLabel']}>Skills</dt>
          <dd className={styles['profileValue']}>
            {volunteer.skills.length > 0 ? (
              <ul className={styles['skillsList']}>
                {volunteer.skills.map((skill) => (
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
          <dd className={styles['profileValue']}>{volunteer.totalEvents}</dd>
        </div>
      </dl>

      {/* Availability */}
      <div className={styles['availabilitySection']}>
        <span
          className={`${styles['availabilityBadge']} ${styles[`availabilityBadge--${displayAvailability.toLowerCase().replace(' ', '_')}`]} ${optimisticAvailability ? styles['availabilityBadge--pending'] : ''}`}
        >
          {AVAILABILITY_LABEL[displayAvailability]}
        </span>
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
        {availabilityError && (
          <span className={styles['availabilityError']} role="alert">{availabilityError}</span>
        )}
      </div>

      {/* Feedback Score Trend Chart */}
      {scoreTrend && scoreTrend.length > 0 && (
        <div className={styles['chartSection']}>
          <h2 className={styles['sectionTitle']}>Feedback Score Trend</h2>
          <div className={styles['chartWrapper']}>
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={scoreTrend} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 12 }}
                  tickFormatter={(val: string) => new Date(val).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                />
                <YAxis domain={[1, 5]} tick={{ fontSize: 12 }} />
                <Tooltip
                  labelFormatter={(val) => new Date(String(val)).toLocaleDateString()}
                  formatter={(val) => [Number(val).toFixed(1), 'Score']}
                />
                <Line type="monotone" dataKey="score" stroke="var(--accent)" strokeWidth={2} dot={{ r: 3 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

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
                  <th scope="col">Role</th>
                  <th scope="col">Score</th>
                </tr>
              </thead>
              <tbody>
                {historyData.content.map((record) => (
                  <tr key={record.eventId}>
                    <td>{record.eventName}</td>
                    <td>{new Date(record.date).toLocaleDateString()}</td>
                    <td>{record.role}</td>
                    <td>{record.feedbackScore !== null ? record.feedbackScore.toFixed(1) : '—'}</td>
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
