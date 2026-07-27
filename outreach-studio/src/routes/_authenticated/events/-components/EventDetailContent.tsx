/**
 * Event Detail Content (lazy-loaded)
 *
 * Displays event details with lifecycle transition buttons.
 * Implements optimistic status updates with rollback on server rejection.
 * Shows valid transitions based on current event status.
 */

import { useState, useCallback } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import type { Event, EventStatus, Volunteer } from '@/types/domain';
import { EVENT_TRANSITIONS } from '@/types/domain';
import type { NormalizedError, PageResponse } from '@/types/api';

import { Route } from '../$eventId';
import type { EventDetailSearch } from '../$eventId';
import styles from './EventDetailContent.module.css';

// ---------------------------------------------------------------------------
// Transition button labels
// ---------------------------------------------------------------------------

const TRANSITION_LABELS: Record<EventStatus, string> = {
  PUBLISHED: 'Publish',
  ACTIVE: 'Activate',
  COMPLETED: 'Complete',
  ARCHIVED: 'Archive',
  CANCELLED: 'Cancel',
  DRAFT: 'Draft',
};

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

function StatusBadge({ status, pending }: { status: EventStatus; pending?: boolean }) {
  const variant = STATUS_VARIANT[status] ?? 'draft';
  return (
    <span className={`${styles['badge']} ${styles[`badge--${variant}`]} ${pending ? styles['badge--pending'] : ''}`}>
      {status}
      {pending && <span className={styles['pendingDot']} aria-hidden="true" />}
    </span>
  );
}

// ---------------------------------------------------------------------------
// Detail field component
// ---------------------------------------------------------------------------

function DetailField({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <div className={styles['detailField']}>
      <dt className={styles['detailLabel']}>{label}</dt>
      <dd className={styles['detailValue']}>{value ?? '—'}</dd>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Volunteers Tab — Enrollment + List
// ---------------------------------------------------------------------------

function VolunteersTab({ eventId }: { eventId: string }) {
  const queryClient = useQueryClient();
  const [enrollEmployeeId, setEnrollEmployeeId] = useState('');
  const [enrollError, setEnrollError] = useState<string | null>(null);
  const [enrollSuccess, setEnrollSuccess] = useState(false);

  // Fetch enrolled volunteers
  const { data: volunteersData, isLoading: volunteersLoading } = useQuery<PageResponse<Volunteer>>({
    queryKey: [...queryKeys.events.detail(eventId), 'volunteers'],
    queryFn: async () => {
      const response = await httpClient.get<PageResponse<Volunteer>>(
        `/events/${eventId}/volunteers`,
        { params: { page: 1, size: 50 } },
      );
      return response.data;
    },
  });

  // Enroll mutation
  const enrollMutation = useMutation<unknown, NormalizedError, string>({
    mutationFn: async (empId) => {
      const response = await httpClient.post(`/events/${eventId}/volunteers`, {
        employeeId: empId,
      });
      return response.data;
    },
    onSuccess: () => {
      setEnrollEmployeeId('');
      setEnrollError(null);
      setEnrollSuccess(true);
      void queryClient.invalidateQueries({
        queryKey: [...queryKeys.events.detail(eventId), 'volunteers'],
      });
      setTimeout(() => setEnrollSuccess(false), 3000);
    },
    onError: (err) => {
      setEnrollSuccess(false);
      // Map specific error types
      if (err.message.toLowerCase().includes('duplicate') || err.message.toLowerCase().includes('already enrolled')) {
        setEnrollError('This volunteer is already enrolled in this event.');
      } else if (err.message.toLowerCase().includes('capacity') || err.message.toLowerCase().includes('full')) {
        setEnrollError('Event is at maximum volunteer capacity.');
      } else if (err.message.toLowerCase().includes('status') || err.message.toLowerCase().includes('invalid')) {
        setEnrollError('Cannot enroll volunteers in the current event status.');
      } else {
        setEnrollError(err.message || 'Enrollment failed.');
      }
    },
  });

  const handleEnroll = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      if (!enrollEmployeeId.trim()) return;
      setEnrollError(null);
      enrollMutation.mutate(enrollEmployeeId.trim());
    },
    [enrollEmployeeId, enrollMutation],
  );

  return (
    <div>
      {/* Enrollment form */}
      <form className={styles['enrollForm']} onSubmit={handleEnroll}>
        <input
          type="text"
          className={styles['enrollInput']}
          placeholder="Enter Employee ID to enroll..."
          value={enrollEmployeeId}
          onChange={(e) => setEnrollEmployeeId(e.target.value)}
          aria-label="Employee ID to enroll"
        />
        <button
          type="submit"
          className={styles['enrollBtn']}
          disabled={enrollMutation.isPending || !enrollEmployeeId.trim()}
        >
          {enrollMutation.isPending ? 'Enrolling...' : 'Enroll'}
        </button>
      </form>
      {enrollSuccess && (
        <p className={styles['enrollSuccess']} role="status">Volunteer enrolled successfully.</p>
      )}
      {enrollError && (
        <p className={styles['enrollError']} role="alert">{enrollError}</p>
      )}

      {/* Enrolled volunteers list */}
      {volunteersLoading && (
        <p className={styles['tabPlaceholder']}>Loading volunteers...</p>
      )}
      {volunteersData && volunteersData.content.length > 0 ? (
        <table className={styles['volunteerTable']}>
          <thead>
            <tr>
              <th scope="col">Employee ID</th>
              <th scope="col">Name</th>
              <th scope="col">Department</th>
              <th scope="col">Availability</th>
            </tr>
          </thead>
          <tbody>
            {volunteersData.content.map((vol) => (
              <tr key={vol.employeeId}>
                <td>{vol.employeeId}</td>
                <td>{vol.name}</td>
                <td>{vol.department}</td>
                <td>{vol.availability}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        !volunteersLoading && (
          <p className={styles['tabPlaceholder']}>No volunteers enrolled yet.</p>
        )
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function EventDetailContent() {
  const { eventId } = Route.useParams();
  const { tab } = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const queryClient = useQueryClient();

  const [optimisticStatus, setOptimisticStatus] = useState<EventStatus | null>(null);
  const [transitionError, setTransitionError] = useState<string | null>(null);

  const handleTabChange = useCallback(
    (newTab: EventDetailSearch['tab']) => {
      void navigate({ search: { tab: newTab } });
    },
    [navigate],
  );

  // Fetch event detail
  const { data: event, isLoading, isError, error, refetch } = useQuery<Event>({
    queryKey: queryKeys.events.detail(eventId),
    queryFn: async () => {
      const response = await httpClient.get<Event>(`/events/${eventId}`);
      return response.data;
    },
  });

  // Status transition mutation with optimistic update
  const transitionMutation = useMutation<Event, NormalizedError, EventStatus>({
    mutationFn: async (targetStatus) => {
      const response = await httpClient.patch<Event>(
        `/events/${eventId}/status`,
        { targetStatus },
      );
      return response.data;
    },
    onMutate: (targetStatus) => {
      // Optimistic: update badge immediately
      setOptimisticStatus(targetStatus);
      setTransitionError(null);
    },
    onSuccess: (updatedEvent) => {
      // Replace cached event with server response
      queryClient.setQueryData(queryKeys.events.detail(eventId), updatedEvent);
      setOptimisticStatus(null);
      void queryClient.invalidateQueries({ queryKey: queryKeys.events.lists() });
    },
    onError: (err) => {
      // Roll back optimistic status
      setOptimisticStatus(null);
      setTransitionError(err.message || 'Status transition failed');
    },
  });

  const handleTransition = useCallback(
    (targetStatus: EventStatus) => {
      transitionMutation.mutate(targetStatus);
    },
    [transitionMutation],
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
  if (isError || !event) {
    return (
      <div className={styles['container']}>
        <div className={styles['errorState']}>
          <p className={styles['errorMessage']}>
            {(error as { message?: string })?.message ?? 'Failed to load event'}
          </p>
          <button
            type="button"
            className={styles['retryBtn']}
            onClick={() => void refetch()}
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const displayStatus = optimisticStatus ?? event.status;
  const validTransitions = EVENT_TRANSITIONS[event.status] ?? [];

  return (
    <div className={styles['container']}>
      {/* Header */}
      <div className={styles['header']}>
        <div>
          <h1 className={styles['pageTitle']}>{event.name}</h1>
          <p className={styles['eventCode']}>{event.code}</p>
        </div>
        <StatusBadge status={displayStatus} pending={optimisticStatus !== null} />
      </div>

      {/* Transition error */}
      {transitionError && (
        <div className={styles['transitionError']} role="alert">
          {transitionError}
        </div>
      )}

      {/* Lifecycle transition buttons */}
      {validTransitions.length > 0 && (
        <div className={styles['transitionBar']}>
          {validTransitions.map((target) => (
            <button
              key={target}
              type="button"
              className={`${styles['transitionBtn']} ${target === 'CANCELLED' ? styles['transitionBtn--danger'] : ''}`}
              onClick={() => handleTransition(target)}
              disabled={transitionMutation.isPending}
            >
              {TRANSITION_LABELS[target] ?? target}
            </button>
          ))}
        </div>
      )}

      {/* Tabs */}
      <nav className={styles['tabs']} aria-label="Event sections">
        {(['overview', 'volunteers', 'feedback', 'notifications', 'audit'] as const).map(
          (tabName) => (
            <button
              key={tabName}
              type="button"
              className={`${styles['tab']} ${tab === tabName ? styles['tab--active'] : ''}`}
              aria-current={tab === tabName ? 'page' : undefined}
              onClick={() => handleTabChange(tabName)}
            >
              {tabName.charAt(0).toUpperCase() + tabName.slice(1)}
            </button>
          ),
        )}
      </nav>

      {/* Tab content — Overview (primary) */}
      {tab === 'overview' && (
        <dl className={styles['detailGrid']}>
          <DetailField label="Description" value={event.description} />
          <DetailField label="Status" value={displayStatus} />
          <DetailField label="Start Date" value={new Date(event.startDate).toLocaleString()} />
          <DetailField label="End Date" value={new Date(event.endDate).toLocaleString()} />
          <DetailField label="City" value={event.city} />
          <DetailField label="Venue" value={event.venue} />
          <DetailField label="Category" value={event.category} />
          <DetailField label="Max Volunteers" value={event.maxVolunteers} />
          <DetailField label="Primary POC" value={event.primaryPoc} />
          <DetailField label="Secondary POC" value={event.secondaryPoc} />
          <DetailField label="Volunteer Count" value={event.volunteerCount} />
          <DetailField label="Attended" value={event.attendedCount} />
          <DetailField label="Not Attended" value={event.notAttendedCount} />
          <DetailField
            label="Avg Feedback Score"
            value={event.averageFeedbackScore !== null ? event.averageFeedbackScore.toFixed(1) : '—'}
          />
        </dl>
      )}

      {/* Volunteers tab with enrollment */}
      {tab === 'volunteers' && (
        <VolunteersTab eventId={eventId} />
      )}
      {tab === 'feedback' && (
        <div className={styles['tabPlaceholder']}>
          <p>Feedback submissions for this event will be shown here.</p>
        </div>
      )}
      {tab === 'notifications' && (
        <div className={styles['tabPlaceholder']}>
          <p>Notification history for this event will be shown here.</p>
        </div>
      )}
      {tab === 'audit' && (
        <div className={styles['tabPlaceholder']}>
          <p>Audit history for this event will be shown here.</p>
        </div>
      )}
    </div>
  );
}
