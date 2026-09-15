/**
 * Event Detail Content (lazy-loaded)
 *
 * Displays event details with lifecycle transition buttons.
 * Implements optimistic status updates with rollback on server rejection.
 * Shows valid transitions based on current event status.
 */

import { useState, useCallback } from 'react';
import { Link, useNavigate } from '@tanstack/react-router';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from '@tanstack/react-form';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useOptimisticMutation } from '@/hooks/useOptimisticMutation';
import { usePermission } from '@/hooks/usePermission';
import { eventCreateSchema } from '@/lib/zod-schemas';
import type { Event, EventStatus, EventEnrollment, FeedbackSubmission } from '@/types/domain';
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
  const { hasPermission: canManage } = usePermission([
    'ROLE_PMO',
    'ROLE_ADMIN',
    'ROLE_TENANT_ADMIN',
    'ROLE_PLATFORM_ADMIN',
  ]);

  const enrollmentsQueryKey = [...queryKeys.events.detail(eventId), 'volunteers'];

  // Fetch enrolled volunteers — not paginated, enrollment lists are small
  // (bounded by the event's maxVolunteers).
  const { data: enrollments, isLoading: volunteersLoading } = useQuery<EventEnrollment[]>({
    queryKey: enrollmentsQueryKey,
    queryFn: async () => {
      const response = await httpClient.get<EventEnrollment[]>(`/events/${eventId}/volunteers`);
      return response.data;
    },
  });

  const enrollMutation = useMutation<unknown, NormalizedError, string>({
    mutationFn: async (empId) => {
      const response = await httpClient.post(`/events/${eventId}/volunteers`, {
        employeeIds: [empId],
      });
      return response.data;
    },
    onSuccess: () => {
      setEnrollEmployeeId('');
      setEnrollError(null);
      setEnrollSuccess(true);
      void queryClient.invalidateQueries({ queryKey: enrollmentsQueryKey });
      setTimeout(() => setEnrollSuccess(false), 3000);
    },
    onError: (err) => {
      setEnrollSuccess(false);
      if (err.message.toLowerCase().includes('duplicate') || err.message.toLowerCase().includes('already enrolled')) {
        setEnrollError('This volunteer is already enrolled in this event.');
      } else if (err.message.toLowerCase().includes('capacity') || err.message.toLowerCase().includes('full')) {
        setEnrollError('Event is at maximum volunteer capacity.');
      } else if (err.message.toLowerCase().includes('status') || err.message.toLowerCase().includes('invalid')) {
        setEnrollError('Cannot enroll volunteers in the current event status.');
      } else if (err.message.toLowerCase().includes('not found')) {
        setEnrollError('No volunteer found with that employee ID.');
      } else {
        setEnrollError(err.message || 'Enrollment failed.');
      }
    },
  });

  const removeMutation = useMutation<unknown, NormalizedError, string>({
    mutationFn: async (empId) => {
      await httpClient.delete(`/events/${eventId}/volunteers/${empId}`);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: enrollmentsQueryKey });
    },
    onError: (err) => {
      setEnrollError(err.message || 'Failed to remove volunteer.');
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
      {canManage && (
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
      )}
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
      {enrollments && enrollments.length > 0 ? (
        <table className={styles['volunteerTable']}>
          <thead>
            <tr>
              <th scope="col">Employee ID</th>
              <th scope="col">Name</th>
              <th scope="col">Attendance</th>
              <th scope="col">Registered At</th>
              {canManage && <th scope="col">Actions</th>}
            </tr>
          </thead>
          <tbody>
            {enrollments.map((enrollment) => (
              <tr key={enrollment.id}>
                <td>{enrollment.employeeId}</td>
                <td>{enrollment.volunteerName}</td>
                <td>{enrollment.attendanceStatus}</td>
                <td>{new Date(enrollment.registeredAt).toLocaleDateString()}</td>
                {canManage && (
                  <td>
                    <button
                      type="button"
                      className={styles['enrollBtn']}
                      disabled={removeMutation.isPending}
                      onClick={() => removeMutation.mutate(enrollment.employeeId)}
                    >
                      Remove
                    </button>
                  </td>
                )}
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
// Feedback Tab — Real data from feedback-service
// ---------------------------------------------------------------------------

const EMOJI_MAP = ['', '😞', '😕', '😐', '🙂', '😄'];

function FeedbackTab({ eventId }: { eventId: string }) {
  const { data, isLoading, isError, error, refetch } = useQuery<PageResponse<FeedbackSubmission>>({
    queryKey: [...queryKeys.feedback.all, 'event', eventId],
    queryFn: async () => {
      const response = await httpClient.get<PageResponse<FeedbackSubmission>>(
        `/feedback/event/${eventId}`,
        // Unlike the app's DataTable-driven lists (which convert their 1-indexed URL page to
        // TanStack Table's 0-based pageIndex before calling the API), this is a bespoke query
        // hitting Spring's raw 0-indexed Pageable directly — page 1 here means the *second*
        // page, which silently looked like "no data" until a real result finally existed to
        // reveal it.
        { params: { page: 0, size: 50 } },
      );
      return response.data;
    },
  });

  // Feedback rows only carry a volunteerId (UUID) — resolve display names from the same
  // event's enrollment list rather than round-tripping through a separate lookup.
  const { data: enrollments } = useQuery<EventEnrollment[]>({
    queryKey: [...queryKeys.events.detail(eventId), 'volunteers'],
    queryFn: async () => {
      const response = await httpClient.get<EventEnrollment[]>(`/events/${eventId}/volunteers`);
      return response.data;
    },
  });
  const volunteerNames = new Map((enrollments ?? []).map((e) => [e.volunteerId, e.volunteerName]));

  // /feedback renders the submission form only for ROLE_POC (ADMIN/PMO get a read-only list
  // there instead) — only show this entry point to viewers who'll actually see the form.
  const { hasPermission: canSubmitFeedback } = usePermission(['ROLE_POC']);
  const giveFeedbackLink = canSubmitFeedback && (
    <Link
      to="/feedback"
      search={{ eventId, page: 1, size: 10 }}
      className={styles['enrollBtn']}
    >
      Give Feedback
    </Link>
  );

  if (isLoading) {
    return <p className={styles['tabPlaceholder']}>Loading feedback...</p>;
  }

  if (isError) {
    return (
      <div className={styles['tabPlaceholder']}>
        <p>{(error as { message?: string })?.message ?? 'Failed to load feedback'}</p>
        <button type="button" className={styles['enrollBtn']} onClick={() => void refetch()}>
          Retry
        </button>
      </div>
    );
  }

  if (!data || data.content.length === 0) {
    return (
      <div>
        {giveFeedbackLink}
        <p className={styles['tabPlaceholder']}>No feedback submissions yet for this event.</p>
      </div>
    );
  }

  return (
    <div>
      {giveFeedbackLink}
      <table className={styles['volunteerTable']}>
        <thead>
          <tr>
            <th scope="col">Volunteer</th>
            <th scope="col">Score</th>
            <th scope="col">Category</th>
            <th scope="col">Submitted At</th>
          </tr>
        </thead>
        <tbody>
          {data.content.map((fb) => (
            <tr key={fb.id}>
              <td>{fb.anonymous ? '(anonymous)' : volunteerNames.get(fb.volunteerId) ?? fb.volunteerId}</td>
              <td>{EMOJI_MAP[fb.score] ?? fb.score}</td>
              <td>{fb.category}</td>
              <td>{new Date(fb.submittedAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Overview Tab — Edit Form
// ---------------------------------------------------------------------------

function validateEditField(fieldName: string, value: unknown): string | undefined {
  const shape = eventCreateSchema._def.schema.shape;
  const fieldSchema = shape[fieldName as keyof typeof shape];
  if (!fieldSchema) return undefined;
  const result = fieldSchema.safeParse(value);
  return result.success ? undefined : result.error.issues[0]?.message;
}

interface EventEditFormProps {
  eventId: string;
  event: Event;
  onCancel: () => void;
  onSaved: () => void;
}

function EventEditForm({ eventId, event, onCancel, onSaved }: EventEditFormProps) {
  const [serverError, setServerError] = useState<string | null>(null);
  const [fieldServerErrors, setFieldServerErrors] = useState<Record<string, string>>({});

  const updateMutation = useMutation({
    mutationFn: async (values: Record<string, unknown>) => {
      const response = await httpClient.put<Event>(`/events/${eventId}`, values);
      return response.data;
    },
    onSuccess: () => {
      onSaved();
    },
    onError: (error: NormalizedError) => {
      if (error.fieldErrors && error.fieldErrors.length > 0) {
        const mapped: Record<string, string> = {};
        for (const fe of error.fieldErrors) {
          mapped[fe.field] = fe.message;
        }
        setFieldServerErrors(mapped);
      } else {
        setServerError(error.message || 'Failed to update event');
      }
    },
  });

  const form = useForm({
    defaultValues: {
      eventName: event.eventName,
      description: event.description,
      eventDate: event.eventDate,
      eventEndDate: event.eventEndDate,
      city: event.city,
      venue: event.venue,
      category: event.category,
      maxVolunteers: event.maxVolunteers,
    },
    onSubmit: ({ value }) => {
      setServerError(null);
      setFieldServerErrors({});
      updateMutation.mutate(value);
    },
  });

  return (
    <form
      className={styles['detailGrid']}
      onSubmit={(e) => {
        e.preventDefault();
        e.stopPropagation();
        void form.handleSubmit();
      }}
    >
      {serverError && (
        <div className={styles['transitionError']} role="alert">
          {serverError}
        </div>
      )}

      {(
        [
          ['eventName', 'Name', 'text'],
          ['description', 'Description', 'text'],
          ['eventDate', 'Event Date', 'date'],
          ['eventEndDate', 'End Date', 'date'],
          ['city', 'City', 'text'],
          ['venue', 'Venue', 'text'],
          ['category', 'Category', 'text'],
        ] as const
      ).map(([name, label, type]) => (
        <form.Field
          key={name}
          name={name}
          validators={{ onBlur: ({ value }) => validateEditField(name, value) }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors[name];
            return (
              <div className={styles['detailField']}>
                <label htmlFor={`edit-${name}`} className={styles['detailLabel']}>
                  {label}
                </label>
                <input
                  id={`edit-${name}`}
                  type={type}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  aria-invalid={hasError}
                />
                {hasError && (
                  <p className={styles['fieldError']} role="alert">
                    {String(errors[0] ?? fieldServerErrors[name])}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>
      ))}

      <form.Field
        name="maxVolunteers"
        validators={{ onBlur: ({ value }) => validateEditField('maxVolunteers', value) }}
      >
        {(field) => {
          const errors = field.state.meta.errors;
          const hasError = errors.length > 0 || !!fieldServerErrors.maxVolunteers;
          return (
            <div className={styles['detailField']}>
              <label htmlFor="edit-maxVolunteers" className={styles['detailLabel']}>
                Max Volunteers
              </label>
              <input
                id="edit-maxVolunteers"
                type="number"
                min={1}
                max={10000}
                value={field.state.value}
                onChange={(e) => field.handleChange(Number(e.target.value))}
                onBlur={field.handleBlur}
                aria-invalid={hasError}
              />
              {hasError && (
                <p className={styles['fieldError']} role="alert">
                  {String(errors[0] ?? fieldServerErrors.maxVolunteers)}
                </p>
              )}
            </div>
          );
        }}
      </form.Field>

      <form.Subscribe selector={(state) => state.values}>
        {(values) => {
          const formValid = eventCreateSchema.safeParse(values).success;
          return (
            <div className={styles['formActions']}>
              <button
                type="submit"
                className={styles['transitionBtn']}
                disabled={!formValid || updateMutation.isPending}
              >
                {updateMutation.isPending ? 'Saving...' : 'Save'}
              </button>
              <button type="button" className={styles['transitionBtn']} onClick={onCancel}>
                Cancel
              </button>
            </div>
          );
        }}
      </form.Subscribe>
    </form>
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
  const [isEditing, setIsEditing] = useState(false);
  const { hasPermission: canManage } = usePermission([
    'ROLE_PMO',
    'ROLE_ADMIN',
    'ROLE_TENANT_ADMIN',
    'ROLE_PLATFORM_ADMIN',
  ]);

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

  // Status transition mutation with optimistic update and 1s rollback timeout
  const transitionMutation = useOptimisticMutation<Event, EventStatus>({
    mutationFn: async (targetStatus) => {
      const response = await httpClient.patch<Event>(
        `/events/${eventId}/status`,
        { targetStatus },
      );
      return response.data;
    },
    queryKey: queryKeys.events.detail(eventId),
    optimisticUpdate: (cached, targetStatus) => {
      if (!cached) return cached;
      setOptimisticStatus(targetStatus);
      setTransitionError(null);
      return { ...cached, status: targetStatus };
    },
    rollbackTimeout: 1000,
    onSuccess: () => {
      setOptimisticStatus(null);
      void queryClient.invalidateQueries({ queryKey: queryKeys.events.lists() });
    },
    onError: (message) => {
      setOptimisticStatus(null);
      setTransitionError(message);
    },
    invalidateKeys: [queryKeys.events.lists()],
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
          <h1 className={styles['pageTitle']}>{event.eventName}</h1>
          <p className={styles['eventCode']}>{event.eventCode}</p>
        </div>
        <div className={styles['headerActions']}>
          <StatusBadge status={displayStatus} pending={optimisticStatus !== null} />
          {canManage && tab === 'overview' && !isEditing && (
            <button
              type="button"
              className={styles['transitionBtn']}
              onClick={() => setIsEditing(true)}
            >
              Edit
            </button>
          )}
        </div>
      </div>

      {/* Transition error */}
      {transitionError && (
        <div className={styles['transitionError']} role="alert">
          {transitionError}
        </div>
      )}

      {/* Lifecycle transition buttons */}
      {canManage && validTransitions.length > 0 && (
        <div className={styles['transitionBar']}>
          {validTransitions.map((target) => (
            <button
              key={target}
              type="button"
              className={`${styles['transitionBtn']} ${target === 'CANCELLED' ? styles['transitionBtn--danger'] : ''}`}
              onClick={() => handleTransition(target)}
              disabled={transitionMutation.isPending}
            >
              {transitionMutation.isPending && optimisticStatus === target && (
                <span className={styles['transitionSpinner']} aria-hidden="true" />
              )}
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
      {tab === 'overview' && isEditing && (
        <EventEditForm
          eventId={eventId}
          event={event}
          onCancel={() => setIsEditing(false)}
          onSaved={() => {
            setIsEditing(false);
            void queryClient.invalidateQueries({ queryKey: queryKeys.events.detail(eventId) });
            void queryClient.invalidateQueries({ queryKey: queryKeys.events.lists() });
          }}
        />
      )}
      {tab === 'overview' && !isEditing && (
        <dl className={styles['detailGrid']}>
          <DetailField label="Description" value={event.description} />
          <DetailField label="Status" value={displayStatus} />
          <DetailField label="Event Date" value={new Date(event.eventDate + 'T00:00:00').toLocaleDateString()} />
          <DetailField label="End Date" value={new Date(event.eventEndDate + 'T00:00:00').toLocaleDateString()} />
          <DetailField label="City" value={event.city} />
          <DetailField label="Venue" value={event.venue} />
          <DetailField label="Category" value={event.category} />
          <DetailField label="Max Volunteers" value={event.maxVolunteers} />
          <DetailField label="Created By" value={event.createdBy} />
          <DetailField label="Registered" value={event.registeredCount} />
          <DetailField label="Attended" value={event.attendedCount} />
        </dl>
      )}

      {/* Volunteers tab with enrollment */}
      {tab === 'volunteers' && (
        <VolunteersTab eventId={eventId} />
      )}
      {tab === 'feedback' && (
        <FeedbackTab eventId={eventId} />
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
