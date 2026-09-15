/**
 * Feedback Form Content (lazy-loaded)
 *
 * Submits feedback for a specific event, on behalf of one of that event's enrolled
 * volunteers — there's no "current user is a volunteer" identity link in this system, so
 * whoever is entering feedback (a POC coordinating the event) picks which enrolled
 * volunteer it's for. Requires `?eventId=` in the URL; without one there's no volunteer
 * list to populate the picker from, so the form isn't rendered at all.
 *
 * - Volunteer picker (populated from the event's enrolled volunteers)
 * - Emoji score (1-5), text answers, category select, optional tags, anonymous toggle
 * - Validates on blur with inline errors within 200ms
 * - Disables submit until all required fields valid
 * - Shows loading indicator on submit button, prevents duplicate submissions
 * - Displays inline success message above form on success
 * - Maps server field-level errors to form fields on validation failure
 * - Shows error notification on network/non-validation failure, preserves form data
 */

import { useState } from 'react';
import { useForm } from '@tanstack/react-form';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { feedbackFormSchema } from '@/lib/zod-schemas';
import type { NormalizedError, PageResponse } from '@/types/api';
import type { EventEnrollment, FeedbackSubmission } from '@/types/domain';

import { Route } from '../index';
import styles from './FeedbackContent.module.css';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const FALLBACK_CATEGORIES = ['Communication', 'Organization', 'Content', 'Logistics', 'Overall'];

const EMOJI_LABELS = ['😞', '😕', '😐', '🙂', '😄'];

// ---------------------------------------------------------------------------
// Field-level validation helpers
// ---------------------------------------------------------------------------

function validateVolunteerId(value: string): string | undefined {
  const result = feedbackFormSchema.shape.volunteerId.safeParse(value);
  if (!result.success) return result.error.issues[0]?.message ?? 'Please select a volunteer';
  return undefined;
}

function validateScore(value: number): string | undefined {
  const result = feedbackFormSchema.shape.score.safeParse(value);
  if (!result.success) return result.error.issues[0]?.message ?? 'Score is required';
  return undefined;
}

function validateAnswer(value: string, fieldName: 'answer1' | 'answer2'): string | undefined {
  const result = feedbackFormSchema.shape[fieldName].safeParse(value);
  if (!result.success) return result.error.issues[0]?.message ?? 'This field is required';
  return undefined;
}

function validateAnswer3(value: string | undefined): string | undefined {
  if (value === undefined || value === '') return undefined;
  const result = feedbackFormSchema.shape.answer3.safeParse(value);
  if (!result.success) return result.error.issues[0]?.message ?? 'Max 500 characters';
  return undefined;
}

function validateCategory(value: string): string | undefined {
  const result = feedbackFormSchema.shape.category.safeParse(value);
  if (!result.success) return result.error.issues[0]?.message ?? 'Category is required';
  return undefined;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function FeedbackContent() {
  const search = Route.useSearch();
  const eventId = search.eventId;
  const queryClient = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);
  const [fieldServerErrors, setFieldServerErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Fetch categories from API, fall back to hardcoded values on error
  const { data: categories } = useQuery<string[]>({
    queryKey: [...queryKeys.feedback.all, 'categories'],
    queryFn: async () => {
      const response = await httpClient.get<string[]>('/feedback/categories');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // Cache for 5 minutes
    placeholderData: FALLBACK_CATEGORIES,
  });

  const FEEDBACK_CATEGORIES = categories ?? FALLBACK_CATEGORIES;

  // Fetch enrolled volunteers for the event feedback is being submitted for — the picker's
  // source of truth. Only enabled once an eventId is present.
  const { data: enrollments, isLoading: enrollmentsLoading } = useQuery<EventEnrollment[]>({
    queryKey: [...queryKeys.events.detail(eventId ?? ''), 'volunteers'],
    queryFn: async () => {
      const response = await httpClient.get<EventEnrollment[]>(`/events/${eventId}/volunteers`);
      return response.data;
    },
    enabled: !!eventId,
  });

  const submitMutation = useMutation({
    mutationFn: async (values: {
      volunteerId: string;
      score: number;
      answer1: string;
      answer2: string;
      answer3?: string;
      category: string;
      tags?: string;
      anonymous: boolean;
    }) => {
      const response = await httpClient.post<FeedbackSubmission>('/feedback', {
        ...values,
        eventId,
      });
      return response.data;
    },
    onMutate: async (values) => {
      // Scoped to .lists() rather than .all: the latter also matches this component's own
      // ['feedback', 'categories'] cache entry (a plain string[], not a PageResponse), and the
      // updater below crashed trying to spread that array's nonexistent .content property.
      // Cancel any outgoing refetches to avoid overwriting optimistic update
      await queryClient.cancelQueries({ queryKey: queryKeys.feedback.lists() });

      // Snapshot previous feedback list data for rollback
      const previousData = queryClient.getQueriesData<PageResponse<FeedbackSubmission>>({
        queryKey: queryKeys.feedback.lists(),
      });

      // Optimistically add new feedback to all matching list caches
      const optimisticEntry: FeedbackSubmission = {
        id: `temp-${Date.now()}`,
        eventId: eventId ?? '',
        volunteerId: values.volunteerId,
        score: values.score,
        answer1: values.answer1,
        answer2: values.answer2,
        answer3: values.answer3,
        category: values.category,
        tags: values.tags,
        status: 'SUBMITTED',
        anonymous: values.anonymous,
        submittedAt: new Date().toISOString(),
      };

      queryClient.setQueriesData<PageResponse<FeedbackSubmission>>(
        { queryKey: queryKeys.feedback.lists() },
        (old) => {
          if (!old) return old;
          return {
            ...old,
            content: [optimisticEntry, ...old.content],
            totalElements: old.totalElements + 1,
          };
        },
      );

      return { previousData };
    },
    onSuccess: () => {
      setSuccessMessage('Feedback submitted successfully. Thank you!');
      setServerError(null);
      setFieldServerErrors({});
      form.reset();
      // Invalidate to fetch real server data
      void queryClient.invalidateQueries({ queryKey: queryKeys.feedback.lists() });
    },
    onError: (error: NormalizedError, _variables, context) => {
      // Rollback optimistic update
      if (context?.previousData) {
        for (const [queryKey, data] of context.previousData) {
          queryClient.setQueryData(queryKey, data);
        }
      }

      setSuccessMessage(null);
      if (error.fieldErrors && error.fieldErrors.length > 0) {
        const mapped: Record<string, string> = {};
        for (const fe of error.fieldErrors) {
          mapped[fe.field] = fe.message;
        }
        setFieldServerErrors(mapped);
        setServerError(null);
      } else {
        setServerError(error.message || 'Failed to submit feedback. Please try again.');
      }
    },
  });

  const form = useForm({
    defaultValues: {
      volunteerId: '',
      score: 0,
      answer1: '',
      answer2: '',
      answer3: '',
      category: '',
      tags: '',
      anonymous: false,
    },
    onSubmit: ({ value }) => {
      setServerError(null);
      setFieldServerErrors({});
      setSuccessMessage(null);
      submitMutation.mutate({
        ...value,
        answer3: value.answer3 || undefined,
        tags: value.tags || undefined,
      });
    },
  });

  if (!eventId) {
    return (
      <div className={styles['container']}>
        <h1 className={styles['pageTitle']}>Submit Feedback</h1>
        <p>
          Feedback is submitted for a specific event. Open an event and use "Give Feedback"
          on its Feedback tab to get here with the right context.
        </p>
        <Link to="/events" search={{ page: 1, size: 10 }} className={styles['submitBtn']}>
          Go to Events
        </Link>
      </div>
    );
  }

  return (
    <div className={styles['container']}>
      <h1 className={styles['pageTitle']}>Submit Feedback</h1>

      {/* Success message above form */}
      {successMessage && (
        <div className={styles['successMessage']} role="status" aria-live="polite">
          {successMessage}
        </div>
      )}

      {/* Network/server error */}
      {serverError && (
        <div className={styles['successMessage']} role="alert" aria-live="assertive" style={{ background: 'var(--color-red-50)', borderColor: 'var(--color-danger-600)', color: 'var(--color-danger-600)' }}>
          {serverError}
        </div>
      )}

      <form
        className={styles['form']}
        onSubmit={(e) => {
          e.preventDefault();
          e.stopPropagation();
          void form.handleSubmit();
        }}
      >
        {/* Volunteer */}
        <form.Field
          name="volunteerId"
          validators={{
            onBlur: ({ value }) => validateVolunteerId(value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.volunteerId;
            return (
              <div className={styles['field']}>
                <label htmlFor="volunteerId" className={styles['label']}>
                  Volunteer<span className={styles['required']}>*</span>
                </label>
                <select
                  id="volunteerId"
                  className={`${styles['select']} ${hasError ? styles['select--error'] : ''}`}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  disabled={enrollmentsLoading}
                  aria-describedby={hasError ? 'volunteerId-error' : undefined}
                  aria-invalid={hasError}
                >
                  <option value="">
                    {enrollmentsLoading ? 'Loading volunteers...' : 'Select a volunteer...'}
                  </option>
                  {(enrollments ?? []).map((enrollment) => (
                    <option key={enrollment.volunteerId} value={enrollment.volunteerId}>
                      {enrollment.volunteerName} ({enrollment.employeeId})
                    </option>
                  ))}
                </select>
                {!enrollmentsLoading && enrollments?.length === 0 && (
                  <span className={styles['charCount']}>
                    No volunteers are enrolled in this event yet.
                  </span>
                )}
                {hasError && (
                  <p id="volunteerId-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.volunteerId)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Emoji Score (1-5) */}
        <form.Field
          name="score"
          validators={{
            onBlur: ({ value }) => validateScore(value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.score;
            return (
              <div className={styles['field']}>
                <span className={styles['label']}>
                  Score<span className={styles['required']}>*</span>
                </span>
                <div className={styles['emojiGroup']} role="radiogroup" aria-label="Feedback score">
                  {EMOJI_LABELS.map((emoji, idx) => {
                    const score = idx + 1;
                    return (
                      <button
                        key={score}
                        type="button"
                        className={`${styles['emojiBtn']} ${field.state.value === score ? styles['emojiBtn--selected'] : ''}`}
                        onClick={() => {
                          field.handleChange(score);
                          field.handleBlur();
                        }}
                        aria-label={`Score ${score}`}
                        aria-pressed={field.state.value === score}
                      >
                        {emoji}
                      </button>
                    );
                  })}
                </div>
                {hasError && (
                  <p className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.score ?? 'Please select a score')}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Answer 1 */}
        <form.Field
          name="answer1"
          validators={{
            onBlur: ({ value }) => validateAnswer(value, 'answer1'),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.answer1;
            return (
              <div className={styles['field']}>
                <label htmlFor="answer1" className={styles['label']}>
                  What went well?<span className={styles['required']}>*</span>
                </label>
                <textarea
                  id="answer1"
                  className={`${styles['textarea']} ${hasError ? styles['textarea--error'] : ''}`}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  maxLength={500}
                  rows={3}
                  aria-describedby={hasError ? 'answer1-error' : undefined}
                  aria-invalid={hasError}
                />
                <span className={styles['charCount']}>{field.state.value.length}/500</span>
                {hasError && (
                  <p id="answer1-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.answer1)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Answer 2 */}
        <form.Field
          name="answer2"
          validators={{
            onBlur: ({ value }) => validateAnswer(value, 'answer2'),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.answer2;
            return (
              <div className={styles['field']}>
                <label htmlFor="answer2" className={styles['label']}>
                  What could be improved?<span className={styles['required']}>*</span>
                </label>
                <textarea
                  id="answer2"
                  className={`${styles['textarea']} ${hasError ? styles['textarea--error'] : ''}`}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  maxLength={500}
                  rows={3}
                  aria-describedby={hasError ? 'answer2-error' : undefined}
                  aria-invalid={hasError}
                />
                <span className={styles['charCount']}>{field.state.value.length}/500</span>
                {hasError && (
                  <p id="answer2-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.answer2)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Answer 3 (optional) */}
        <form.Field
          name="answer3"
          validators={{
            onBlur: ({ value }) => validateAnswer3(value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.answer3;
            return (
              <div className={styles['field']}>
                <label htmlFor="answer3" className={styles['label']}>
                  Additional comments (optional)
                </label>
                <textarea
                  id="answer3"
                  className={`${styles['textarea']} ${hasError ? styles['textarea--error'] : ''}`}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  maxLength={500}
                  rows={3}
                  aria-describedby={hasError ? 'answer3-error' : undefined}
                  aria-invalid={hasError}
                />
                <span className={styles['charCount']}>{(field.state.value ?? '').length}/500</span>
                {hasError && (
                  <p id="answer3-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.answer3)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Category */}
        <form.Field
          name="category"
          validators={{
            onBlur: ({ value }) => validateCategory(value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.category;
            return (
              <div className={styles['field']}>
                <label htmlFor="category" className={styles['label']}>
                  Category<span className={styles['required']}>*</span>
                </label>
                <select
                  id="category"
                  className={`${styles['select']} ${hasError ? styles['select--error'] : ''}`}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  aria-describedby={hasError ? 'category-error' : undefined}
                  aria-invalid={hasError}
                >
                  <option value="">Select a category...</option>
                  {FEEDBACK_CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
                {hasError && (
                  <p id="category-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.category)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Tags (optional) */}
        <form.Field name="tags">
          {(field) => (
            <div className={styles['field']}>
              <label htmlFor="tags" className={styles['label']}>
                Tags (optional)
              </label>
              <input
                id="tags"
                type="text"
                className={styles['textarea']}
                placeholder="e.g. positive, organized"
                value={field.state.value}
                onChange={(e) => field.handleChange(e.target.value)}
                onBlur={field.handleBlur}
                maxLength={200}
              />
            </div>
          )}
        </form.Field>

        {/* Anonymous toggle */}
        <form.Field name="anonymous">
          {(field) => (
            <div className={styles['toggleField']}>
              <input
                id="anonymous"
                type="checkbox"
                className={styles['toggle']}
                checked={field.state.value}
                onChange={(e) => field.handleChange(e.target.checked)}
              />
              <label htmlFor="anonymous" className={styles['toggleLabel']}>
                Submit anonymously
              </label>
            </div>
          )}
        </form.Field>

        {/* Submit button — wrapped in form.Subscribe so it re-renders on every field change;
            reading form.state.values directly in the component body (the previous approach)
            doesn't subscribe to updates, so the button would compute validity once at mount
            (all fields empty → invalid) and never re-evaluate, staying disabled forever. */}
        <form.Subscribe selector={(state) => state.values}>
          {(values) => {
            const isFormValid = feedbackFormSchema.safeParse({
              ...values,
              answer3: values.answer3 || undefined,
              tags: values.tags || undefined,
            }).success;
            return (
              <button
                type="submit"
                className={styles['submitBtn']}
                disabled={!isFormValid || submitMutation.isPending}
              >
                {submitMutation.isPending && <span className={styles['spinner']} aria-hidden="true" />}
                {submitMutation.isPending ? 'Submitting...' : 'Submit Feedback'}
              </button>
            );
          }}
        </form.Subscribe>
      </form>
    </div>
  );
}
