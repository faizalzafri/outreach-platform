/**
 * Team Create/Edit Form
 *
 * Shared between the team list page's "Create Team" section and the team
 * detail page's "Edit" toggle. Pre-populated via `initialValues` in edit mode.
 */

import { useForm } from '@tanstack/react-form';

import { teamFormSchema, type TeamFormValues } from '@/lib/zod-schemas';

import styles from './TeamForm.module.css';

function validateField(fieldName: keyof TeamFormValues, value: unknown): string | undefined {
  const shape = teamFormSchema.shape;
  const fieldSchema = shape[fieldName];
  const result = fieldSchema.safeParse(value);
  return result.success ? undefined : result.error.issues[0]?.message;
}

function isFormValid(values: TeamFormValues): boolean {
  return teamFormSchema.safeParse(values).success;
}

export interface TeamFormProps {
  mode: 'create' | 'edit';
  initialValues?: TeamFormValues;
  onSubmit: (values: TeamFormValues) => void;
  onCancel: () => void;
  isSubmitting: boolean;
  fieldServerErrors?: Record<string, string>;
}

export function TeamForm({
  mode,
  initialValues,
  onSubmit,
  onCancel,
  isSubmitting,
  fieldServerErrors = {},
}: TeamFormProps) {
  const form = useForm({
    defaultValues: initialValues ?? { name: '', description: '' },
    onSubmit: ({ value }) => onSubmit(value),
  });

  return (
    <form
      className={styles['form']}
      onSubmit={(e) => {
        e.preventDefault();
        e.stopPropagation();
        void form.handleSubmit();
      }}
    >
      <form.Field name="name" validators={{ onBlur: ({ value }) => validateField('name', value) }}>
        {(field) => {
          const errors = field.state.meta.errors;
          const hasError = errors.length > 0 || Boolean(fieldServerErrors.name);
          return (
            <div className={styles['fieldGroup']}>
              <label htmlFor={`team-name-${mode}`} className={styles['label']}>
                Name
              </label>
              <input
                id={`team-name-${mode}`}
                type="text"
                className={styles['input']}
                value={field.state.value}
                onChange={(e) => field.handleChange(e.target.value)}
                onBlur={field.handleBlur}
                maxLength={100}
                aria-describedby={hasError ? `team-name-${mode}-error` : undefined}
                aria-invalid={hasError}
              />
              {hasError && (
                <p id={`team-name-${mode}-error`} className={styles['fieldError']} role="alert" aria-live="assertive">
                  {String(errors[0] ?? fieldServerErrors.name)}
                </p>
              )}
            </div>
          );
        }}
      </form.Field>

      <form.Field
        name="description"
        validators={{ onBlur: ({ value }) => validateField('description', value) }}
      >
        {(field) => {
          const errors = field.state.meta.errors;
          const hasError = errors.length > 0 || Boolean(fieldServerErrors.description);
          return (
            <div className={styles['fieldGroup']}>
              <label htmlFor={`team-description-${mode}`} className={styles['label']}>
                Description
              </label>
              <textarea
                id={`team-description-${mode}`}
                className={styles['textarea']}
                value={field.state.value}
                onChange={(e) => field.handleChange(e.target.value)}
                onBlur={field.handleBlur}
                maxLength={500}
                rows={3}
                aria-describedby={hasError ? `team-description-${mode}-error` : undefined}
                aria-invalid={hasError}
              />
              {hasError && (
                <p
                  id={`team-description-${mode}-error`}
                  className={styles['fieldError']}
                  role="alert"
                  aria-live="assertive"
                >
                  {String(errors[0] ?? fieldServerErrors.description)}
                </p>
              )}
            </div>
          );
        }}
      </form.Field>

      <form.Subscribe selector={(state) => state.values}>
        {(values) => {
          const formValid = isFormValid(values);
          return (
            <div className={styles['formActions']}>
              <button
                type="submit"
                className={styles['submitBtn']}
                disabled={!formValid || isSubmitting}
                aria-busy={isSubmitting}
              >
                {isSubmitting && <span className={styles['spinner']} aria-hidden="true" />}
                {isSubmitting ? 'Saving...' : mode === 'create' ? 'Create Team' : 'Save Changes'}
              </button>
              <button type="button" className={styles['cancelBtn']} onClick={onCancel}>
                Cancel
              </button>
            </div>
          );
        }}
      </form.Subscribe>
    </form>
  );
}
