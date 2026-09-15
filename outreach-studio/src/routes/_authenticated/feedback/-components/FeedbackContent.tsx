/**
 * Feedback Form Content (lazy-loaded)
 *
 * Form for submitting event feedback using TanStack Form + Zod validation.
 * - Emoji score (1-5), text answers, category select, anonymous toggle
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

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { feedbackFormSchema } from '@/lib/zod-schemas';
import type { NormalizedError, PageResponse } from '@/types/api';
import type { FeedbackSubmission } from '@/types/domain';

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

function validateEmojiScore(value: number): string | undefined {
  const result = feedbackFormSchema.shape.emojiScore.safeParse(value);
  if (!result.success) return result.error.issues[0]?.message ?? 'Score is required';
  return undefined;
}

function validateTextAnswer(value: string, fieldName: 'textAnswer1' | 'textAnswer2'): string | undefined {
  const result = feedbackFormSchema.shape[fieldName].safeParse(value);
  if (!result.success) return result.error.issues[0]?.message ?? 'This field is required';
  return undefined;
}

function validateTextAnswer3(value: string | undefined): string | undefined {
  if (value === undefined || value === '') return undefined;
  const result = feedbackFormSchema.shape.textAnswer3.safeParse(value);
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

  const submitMutation = useMutation({
    mutationFn: async (values: {
      emojiScore: number;
      textAnswer1: string;
      textAnswer2: string;
      textAnswer3?: string;
      category: string;
      anonymous: boolean;
    }) => {
      const response = await httpClient.post<FeedbackSubmission>('/feedback', {
        ...values,
        eventId: search.eventId ?? '',
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
        eventId: search.eventId ?? '',
        employeeId: '',
        emojiScore: values.emojiScore,
        textAnswer1: values.textAnswer1,
        textAnswer2: values.textAnswer2,
        textAnswer3: values.textAnswer3,
        category: values.category,
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
      emojiScore: 0,
      textAnswer1: '',
      textAnswer2: '',
      textAnswer3: '',
      category: '',
      anonymous: false,
    },
    onSubmit: ({ value }) => {
      setServerError(null);
      setFieldServerErrors({});
      setSuccessMessage(null);
      submitMutation.mutate({
        ...value,
        textAnswer3: value.textAnswer3 || undefined,
      });
    },
  });

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
        {/* Emoji Score (1-5) */}
        <form.Field
          name="emojiScore"
          validators={{
            onBlur: ({ value }) => validateEmojiScore(value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.emojiScore;
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
                    {String(errors[0] ?? fieldServerErrors.emojiScore ?? 'Please select a score')}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Text Answer 1 */}
        <form.Field
          name="textAnswer1"
          validators={{
            onBlur: ({ value }) => validateTextAnswer(value, 'textAnswer1'),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.textAnswer1;
            return (
              <div className={styles['field']}>
                <label htmlFor="textAnswer1" className={styles['label']}>
                  What went well?<span className={styles['required']}>*</span>
                </label>
                <textarea
                  id="textAnswer1"
                  className={`${styles['textarea']} ${hasError ? styles['textarea--error'] : ''}`}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  maxLength={500}
                  rows={3}
                  aria-describedby={hasError ? 'textAnswer1-error' : undefined}
                  aria-invalid={hasError}
                />
                <span className={styles['charCount']}>{field.state.value.length}/500</span>
                {hasError && (
                  <p id="textAnswer1-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.textAnswer1)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Text Answer 2 */}
        <form.Field
          name="textAnswer2"
          validators={{
            onBlur: ({ value }) => validateTextAnswer(value, 'textAnswer2'),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.textAnswer2;
            return (
              <div className={styles['field']}>
                <label htmlFor="textAnswer2" className={styles['label']}>
                  What could be improved?<span className={styles['required']}>*</span>
                </label>
                <textarea
                  id="textAnswer2"
                  className={`${styles['textarea']} ${hasError ? styles['textarea--error'] : ''}`}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  maxLength={500}
                  rows={3}
                  aria-describedby={hasError ? 'textAnswer2-error' : undefined}
                  aria-invalid={hasError}
                />
                <span className={styles['charCount']}>{field.state.value.length}/500</span>
                {hasError && (
                  <p id="textAnswer2-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.textAnswer2)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Text Answer 3 (optional) */}
        <form.Field
          name="textAnswer3"
          validators={{
            onBlur: ({ value }) => validateTextAnswer3(value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.textAnswer3;
            return (
              <div className={styles['field']}>
                <label htmlFor="textAnswer3" className={styles['label']}>
                  Additional comments (optional)
                </label>
                <textarea
                  id="textAnswer3"
                  className={`${styles['textarea']} ${hasError ? styles['textarea--error'] : ''}`}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  maxLength={500}
                  rows={3}
                  aria-describedby={hasError ? 'textAnswer3-error' : undefined}
                  aria-invalid={hasError}
                />
                <span className={styles['charCount']}>{(field.state.value ?? '').length}/500</span>
                {hasError && (
                  <p id="textAnswer3-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.textAnswer3)}
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
              textAnswer3: values.textAnswer3 || undefined,
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
