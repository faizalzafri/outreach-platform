/**
 * Event Create Content (lazy-loaded)
 *
 * Form for creating a new event using TanStack Form + Zod validation.
 * Validates on blur with inline error messages, maps server field-level
 * validation errors to form fields on submission failure.
 * Submit button is disabled until all required fields pass validation.
 */

import { useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { useForm } from '@tanstack/react-form';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { eventCreateSchema } from '@/lib/zod-schemas';
import type { NormalizedError } from '@/types/api';
import type { Event } from '@/types/domain';

import styles from './EventCreateContent.module.css';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function validateField(fieldName: string, value: unknown): string | undefined {
  const shape = eventCreateSchema._def.schema.shape;
  const fieldSchema = shape[fieldName as keyof typeof shape];
  if (!fieldSchema) return undefined;
  const result = fieldSchema.safeParse(value);
  if (!result.success) {
    return result.error.issues[0]?.message;
  }
  return undefined;
}

function validateEndDate(endDate: string, startDate: string): string | undefined {
  if (!endDate) return 'Required';
  if (startDate && endDate && new Date(endDate) < new Date(startDate)) {
    return 'End date must be on or after start date';
  }
  return undefined;
}

/**
 * Checks whether the full form values pass the Zod schema.
 * Used to determine if the submit button should be enabled.
 */
function isFormValid(values: {
  eventName: string;
  description: string;
  eventDate: string;
  eventEndDate: string;
  city: string;
  venue: string;
  category: string;
  maxVolunteers: number;
}): boolean {
  const result = eventCreateSchema.safeParse(values);
  return result.success;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function EventCreateContent() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);
  const [fieldServerErrors, setFieldServerErrors] = useState<Record<string, string>>({});

  const createMutation = useMutation({
    mutationFn: async (values: Record<string, unknown>) => {
      const response = await httpClient.post<Event>('/events', values);
      return response.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.events.all });
      void navigate({ to: '/events', search: { page: 1, size: 10 } });
    },
    onError: (error: NormalizedError) => {
      if (error.fieldErrors && error.fieldErrors.length > 0) {
        const mapped: Record<string, string> = {};
        for (const fe of error.fieldErrors) {
          mapped[fe.field] = fe.message;
        }
        setFieldServerErrors(mapped);
      } else {
        setServerError(error.message || 'Failed to create event');
      }
    },
  });

  const form = useForm({
    defaultValues: {
      eventName: '',
      description: '',
      eventDate: '',
      eventEndDate: '',
      city: '',
      venue: '',
      category: '',
      maxVolunteers: 1,
    },
    onSubmit: ({ value }) => {
      setServerError(null);
      setFieldServerErrors({});
      createMutation.mutate(value);
    },
  });

  return (
    <div className={styles['container']}>
      <h1 className={styles['pageTitle']}>Create Event</h1>

      {serverError && (
        <div className={styles['serverError']} role="alert">
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
        {/* Name */}
        <form.Field
          name="eventName"
          validators={{
            onBlur: ({ value }) => validateField('eventName', value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.eventName;
            return (
              <div className={styles['fieldGroup']}>
                <label htmlFor="eventName" className={styles['label']}>
                  Name
                </label>
                <input
                  id="eventName"
                  type="text"
                  className={styles['input']}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  maxLength={255}
                  aria-describedby={hasError ? 'eventName-error' : undefined}
                  aria-invalid={hasError}
                />
                {hasError && (
                  <p id="eventName-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.eventName)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Description */}
        <form.Field
          name="description"
          validators={{
            onBlur: ({ value }) => validateField('description', value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.description;
            return (
              <div className={styles['fieldGroup']}>
                <label htmlFor="description" className={styles['label']}>
                  Description
                </label>
                <textarea
                  id="description"
                  className={styles['textarea']}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  maxLength={2000}
                  rows={4}
                  aria-describedby={hasError ? 'description-error' : undefined}
                  aria-invalid={hasError}
                />
                {hasError && (
                  <p id="description-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.description)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Date Range */}
        <div className={styles['fieldRow']}>
          <form.Field
            name="eventDate"
            validators={{
              onBlur: ({ value }) => {
                if (!value) return 'Event date is required';
                return undefined;
              },
            }}
          >
            {(field) => {
              const errors = field.state.meta.errors;
              const hasError = errors.length > 0;
              return (
                <div className={styles['fieldGroup']}>
                  <label htmlFor="eventDate" className={styles['label']}>
                    Event Date
                  </label>
                  <input
                    id="eventDate"
                    type="date"
                    className={styles['input']}
                    value={field.state.value}
                    onChange={(e) => field.handleChange(e.target.value)}
                    onBlur={field.handleBlur}
                    aria-describedby={hasError ? 'eventDate-error' : undefined}
                    aria-invalid={hasError}
                  />
                  {hasError && (
                    <p id="eventDate-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                      {String(errors[0])}
                    </p>
                  )}
                </div>
              );
            }}
          </form.Field>

          <form.Field
            name="eventEndDate"
            validators={{
              onBlur: ({ value, fieldApi }) => {
                const eventDate = fieldApi.form.getFieldValue('eventDate');
                return validateEndDate(value, eventDate);
              },
            }}
          >
            {(field) => {
              const errors = field.state.meta.errors;
              const hasError = errors.length > 0 || !!fieldServerErrors.eventEndDate;
              return (
                <div className={styles['fieldGroup']}>
                  <label htmlFor="eventEndDate" className={styles['label']}>
                    End Date
                  </label>
                  <input
                    id="eventEndDate"
                    type="date"
                    className={styles['input']}
                    value={field.state.value}
                    onChange={(e) => field.handleChange(e.target.value)}
                    onBlur={field.handleBlur}
                    aria-describedby={hasError ? 'eventEndDate-error' : undefined}
                    aria-invalid={hasError}
                  />
                  {hasError && (
                    <p id="eventEndDate-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                      {String(errors[0] ?? fieldServerErrors.eventEndDate)}
                    </p>
                  )}
                </div>
              );
            }}
          </form.Field>
        </div>

        {/* City */}
        <form.Field
          name="city"
          validators={{
            onBlur: ({ value }) => validateField('city', value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.city;
            return (
              <div className={styles['fieldGroup']}>
                <label htmlFor="city" className={styles['label']}>
                  City
                </label>
                <input
                  id="city"
                  type="text"
                  className={styles['input']}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  aria-describedby={hasError ? 'city-error' : undefined}
                  aria-invalid={hasError}
                />
                {hasError && (
                  <p id="city-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.city)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Venue */}
        <form.Field
          name="venue"
          validators={{
            onBlur: ({ value }) => validateField('venue', value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.venue;
            return (
              <div className={styles['fieldGroup']}>
                <label htmlFor="venue" className={styles['label']}>
                  Venue
                </label>
                <input
                  id="venue"
                  type="text"
                  className={styles['input']}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  aria-describedby={hasError ? 'venue-error' : undefined}
                  aria-invalid={hasError}
                />
                {hasError && (
                  <p id="venue-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.venue)}
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
            onBlur: ({ value }) => validateField('category', value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.category;
            return (
              <div className={styles['fieldGroup']}>
                <label htmlFor="category" className={styles['label']}>
                  Category
                </label>
                <input
                  id="category"
                  type="text"
                  className={styles['input']}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
                  onBlur={field.handleBlur}
                  aria-describedby={hasError ? 'category-error' : undefined}
                  aria-invalid={hasError}
                />
                {hasError && (
                  <p id="category-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.category)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Max Volunteers */}
        <form.Field
          name="maxVolunteers"
          validators={{
            onBlur: ({ value }) => validateField('maxVolunteers', value),
          }}
        >
          {(field) => {
            const errors = field.state.meta.errors;
            const hasError = errors.length > 0 || !!fieldServerErrors.maxVolunteers;
            return (
              <div className={styles['fieldGroup']}>
                <label htmlFor="maxVolunteers" className={styles['label']}>
                  Max Volunteers
                </label>
                <input
                  id="maxVolunteers"
                  type="number"
                  className={styles['input']}
                  value={field.state.value}
                  onChange={(e) => field.handleChange(Number(e.target.value))}
                  onBlur={field.handleBlur}
                  min={1}
                  max={10000}
                  aria-describedby={hasError ? 'maxVolunteers-error' : undefined}
                  aria-invalid={hasError}
                />
                <span className={styles['fieldHint']}>Between 1 and 10,000</span>
                {hasError && (
                  <p id="maxVolunteers-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                    {String(errors[0] ?? fieldServerErrors.maxVolunteers)}
                  </p>
                )}
              </div>
            );
          }}
        </form.Field>

        {/* Submit */}
        <form.Subscribe selector={(state) => state.values}>
          {(values) => {
            const formValid = isFormValid(values);
            return (
              <div className={styles['formActions']}>
                <button
                  type="submit"
                  className={styles['submitBtn']}
                  disabled={!formValid || createMutation.isPending}
                  aria-busy={createMutation.isPending}
                >
                  {createMutation.isPending && (
                    <span className={styles['spinner']} aria-hidden="true" />
                  )}
                  {createMutation.isPending ? 'Creating...' : 'Create Event'}
                </button>
                <button
                  type="button"
                  className={styles['cancelBtn']}
                  onClick={() => void navigate({ to: '/events', search: { page: 1, size: 10 } })}
                >
                  Cancel
                </button>
              </div>
            );
          }}
        </form.Subscribe>
      </form>
    </div>
  );
}
