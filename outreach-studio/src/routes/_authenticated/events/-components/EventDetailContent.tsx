/**
 * Event Detail Content (lazy-loaded)
 *
 * Displays event details with lifecycle transition buttons.
 * Implements optimistic status updates with rollback on server rejection.
 * Shows valid transitions based on current event status.
 */

import { useState, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import type { Event, EventStatus } from '@/types/domain';
import { EVENT_TRANSITIONS } from '@/types/domain';
import type { NormalizedError } from '@/types/api';

import { Route } from '../$eventId';
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
// Component
// ---------------------------------------------------------------------------

export function EventDetailContent() {
  const { eventId } = Route.useParams();
  const { tab } = Route.useSearch();
  const queryClient = useQueryClient();

  const [optimisticStatus, setOptimisticStatus] = useState<EventStatus | null>(null);
  const [transitionError, setTransitionError] = useState<string | null>(null);

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

      {/* Placeholder tab content */}
      {tab === 'volunteers' && (
        <div className={styles['tabPlaceholder']}>
          <p>Volunteer enrollment and list will be shown here.</p>
        </div>
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
