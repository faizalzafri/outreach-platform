/**
 * Admin Content (lazy-loaded)
 *
 * User Administration page with:
 * - User list in DataTable (username, email, role, status, last login, actions)
 * - Create user form with validation (username, email, role)
 * - Role change via PATCH /admin/users/{id}/role
 * - Status change (Disable/Enable) via PATCH /admin/users/{id}/status with confirmation
 * - Unlock action for locked accounts
 * - Success/error toasts on mutations
 * - List refresh on successful mutations
 */

import { useState, useCallback, useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from '@tanstack/react-form';
import type { ColumnDef } from '@tanstack/react-table';

import { DataTable } from '@/components/data-table/DataTable';
import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { userCreateSchema } from '@/lib/zod-schemas';
import { useToast } from '@/hooks/useToast';
import type { NormalizedError } from '@/types/api';
import type { User, UserRole, UserStatus } from '@/types/domain';

import { Route } from '../index';
import styles from './AdminContent.module.css';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const ROLE_OPTIONS: { label: string; value: UserRole }[] = [
  { label: 'Admin', value: 'ROLE_ADMIN' },
  { label: 'PMO', value: 'ROLE_PMO' },
  { label: 'POC', value: 'ROLE_POC' },
];

function StatusBadge({ status }: { status: UserStatus }) {
  const variant = status.toLowerCase();
  return (
    <span className={`${styles['badge']} ${styles[`badge--${variant}`]}`}>
      {status}
    </span>
  );
}

function deriveStatus(user: User): UserStatus {
  return user.enabled ? 'ENABLED' : 'DISABLED';
}

function validateField(fieldName: string, value: unknown): string | undefined {
  const shape = userCreateSchema.shape;
  const fieldSchema = shape[fieldName as keyof typeof shape];
  if (!fieldSchema) return undefined;
  const result = fieldSchema.safeParse(value);
  if (!result.success) {
    return result.error.issues[0]?.message;
  }
  return undefined;
}

function isFormValid(values: { username: string; email: string; role: string }): boolean {
  return userCreateSchema.safeParse(values).success;
}

// ---------------------------------------------------------------------------
// Confirmation Dialog
// ---------------------------------------------------------------------------

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  onConfirm: () => void;
  onCancel: () => void;
}

function ConfirmDialog({ open, title, message, confirmLabel, onConfirm, onCancel }: ConfirmDialogProps) {
  if (!open) return null;

  return (
    <div className={styles['dialogOverlay']} onClick={onCancel} role="presentation">
      <div
        className={styles['dialog']}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        aria-describedby="confirm-dialog-message"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="confirm-dialog-title" className={styles['dialogTitle']}>
          {title}
        </h2>
        <p id="confirm-dialog-message" className={styles['dialogMessage']}>
          {message}
        </p>
        <div className={styles['dialogActions']}>
          <button
            type="button"
            className={styles['dialogCancelBtn']}
            onClick={onCancel}
          >
            Cancel
          </button>
          <button
            type="button"
            className={styles['dialogConfirmBtn']}
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function AdminContent() {
  const search = Route.useSearch();
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();

  // --- Create user state ---
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [fieldServerErrors, setFieldServerErrors] = useState<Record<string, string>>({});

  // --- Confirmation dialog state ---
  const [confirmDialog, setConfirmDialog] = useState<{
    open: boolean;
    title: string;
    message: string;
    confirmLabel: string;
    onConfirm: () => void;
  }>({ open: false, title: '', message: '', confirmLabel: '', onConfirm: () => {} });

  // --- Mutations ---

  const createUserMutation = useMutation({
    mutationFn: async (values: { username: string; email: string; role: string }) => {
      const response = await httpClient.post<User>('/admin/users', values);
      return response.data;
    },
    onSuccess: () => {
      toastSuccess('User created successfully');
      void queryClient.invalidateQueries({ queryKey: queryKeys.admin.all });
      setShowCreateForm(false);
      setCreateError(null);
      setFieldServerErrors({});
    },
    onError: (error: NormalizedError) => {
      if (error.fieldErrors && error.fieldErrors.length > 0) {
        const mapped: Record<string, string> = {};
        for (const fe of error.fieldErrors) {
          mapped[fe.field] = fe.message;
        }
        setFieldServerErrors(mapped);
      } else {
        setCreateError(error.message || 'Failed to create user');
        toastError(error.message || 'Failed to create user');
      }
    },
  });

  const changeRoleMutation = useMutation({
    mutationFn: async ({ userId, role }: { userId: string; role: UserRole }) => {
      const response = await httpClient.patch<User>(`/admin/users/${userId}/role`, { role });
      return response.data;
    },
    onSuccess: () => {
      toastSuccess('Role updated successfully');
      void queryClient.invalidateQueries({ queryKey: queryKeys.admin.all });
    },
    onError: (error: NormalizedError) => {
      toastError(error.message || 'Failed to update role');
    },
  });

  const changeStatusMutation = useMutation({
    mutationFn: async ({ userId, status }: { userId: string; status: UserStatus }) => {
      const response = await httpClient.patch<User>(`/admin/users/${userId}/status`, { status });
      return response.data;
    },
    onSuccess: () => {
      toastSuccess('Account status updated successfully');
      void queryClient.invalidateQueries({ queryKey: queryKeys.admin.all });
    },
    onError: (error: NormalizedError) => {
      toastError(error.message || 'Failed to update account status');
    },
  });

  // --- Handlers ---

  const handleRoleChange = useCallback(
    (userId: string, newRole: UserRole) => {
      changeRoleMutation.mutate({ userId, role: newRole });
    },
    [changeRoleMutation],
  );

  const handleStatusChange = useCallback(
    (userId: string, username: string, targetStatus: UserStatus) => {
      const action = targetStatus === 'DISABLED' ? 'Disable' : 'Enable';
      setConfirmDialog({
        open: true,
        title: `${action} Account`,
        message: `Are you sure you want to ${action.toLowerCase()} the account for "${username}"?`,
        confirmLabel: action,
        onConfirm: () => {
          changeStatusMutation.mutate({ userId, status: targetStatus });
          setConfirmDialog((prev) => ({ ...prev, open: false }));
        },
      });
    },
    [changeStatusMutation],
  );

  // --- Column definitions ---

  const columns: ColumnDef<User, unknown>[] = useMemo(
    () => [
      {
        accessorKey: 'username',
        header: 'Username',
        enableSorting: true,
        enableColumnFilter: false,
      },
      {
        accessorKey: 'email',
        header: 'Email',
        enableSorting: true,
        enableColumnFilter: false,
      },
      {
        accessorKey: 'role',
        header: 'Role',
        enableSorting: true,
        enableColumnFilter: true,
        meta: {
          filterType: 'select',
          filterOptions: ROLE_OPTIONS.map((r) => ({ label: r.label, value: r.value })),
        },
        cell: ({ row }) => (
          <select
            className={styles['roleSelect']}
            value={row.original.role}
            onChange={(e) => handleRoleChange(row.original.id, e.target.value as UserRole)}
            aria-label={`Change role for ${row.original.username}`}
          >
            {ROLE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        ),
      },
      {
        accessorKey: 'enabled',
        header: 'Status',
        enableSorting: true,
        enableColumnFilter: true,
        meta: {
          filterType: 'select',
          filterOptions: [
            { label: 'Enabled', value: 'true' },
            { label: 'Disabled', value: 'false' },
          ],
        },
        cell: ({ row }) => <StatusBadge status={deriveStatus(row.original)} />,
      },
      {
        id: 'actions',
        header: 'Actions',
        enableSorting: false,
        enableColumnFilter: false,
        enableHiding: false,
        cell: ({ row }) => {
          const user = row.original;
          const status = deriveStatus(user);
          return (
            <div className={styles['actions']}>
              {/* Status toggle: Disable/Enable */}
              {status === 'ENABLED' && (
                <button
                  type="button"
                  className={styles['actionBtnDanger']}
                  onClick={() => handleStatusChange(user.id, user.username, 'DISABLED')}
                  aria-label={`Disable account for ${user.username}`}
                >
                  Disable
                </button>
              )}
              {status === 'DISABLED' && (
                <button
                  type="button"
                  className={styles['actionBtn']}
                  onClick={() => handleStatusChange(user.id, user.username, 'ENABLED')}
                  aria-label={`Enable account for ${user.username}`}
                >
                  Enable
                </button>
              )}
            </div>
          );
        },
      },
    ],
    [handleRoleChange, handleStatusChange],
  );

  // --- Query key for DataTable ---

  const queryKey = useMemo(
    () => queryKeys.admin.users({
      page: search.page,
      size: search.size,
      role: search.role,
      status: search.status,
    }),
    [search.page, search.size, search.role, search.status],
  );

  // --- Create user form ---

  const form = useForm({
    defaultValues: {
      username: '',
      email: '',
      role: 'ROLE_POC' as string,
    },
    onSubmit: ({ value }) => {
      setCreateError(null);
      setFieldServerErrors({});
      createUserMutation.mutate(value);
    },
  });

  return (
    <div className={styles['container']}>
      <div className={styles['header']}>
        <h1 className={styles['pageTitle']}>User Administration</h1>
        <button
          type="button"
          className={styles['toggleBtn']}
          onClick={() => setShowCreateForm((prev) => !prev)}
        >
          {showCreateForm ? 'Cancel' : 'Create User'}
        </button>
      </div>

      {/* Create User Form */}
      {showCreateForm && (
        <div className={styles['createSection']}>
          <h2 className={styles['createSectionTitle']}>Create New User</h2>

          {createError && (
            <div className={styles['serverError']} role="alert">
              {createError}
            </div>
          )}

          <form
            className={styles['createForm']}
            onSubmit={(e) => {
              e.preventDefault();
              e.stopPropagation();
              void form.handleSubmit();
            }}
          >
            {/* Username */}
            <form.Field
              name="username"
              validators={{
                onBlur: ({ value }) => validateField('username', value),
              }}
            >
              {(field) => {
                const errors = field.state.meta.errors;
                const hasError = errors.length > 0 || !!fieldServerErrors.username;
                return (
                  <div className={styles['fieldGroup']}>
                    <label htmlFor="create-username" className={styles['label']}>
                      Username
                    </label>
                    <input
                      id="create-username"
                      type="text"
                      className={styles['input']}
                      placeholder="3-50 chars, alphanumeric + underscore"
                      value={field.state.value}
                      onChange={(e) => field.handleChange(e.target.value)}
                      onBlur={field.handleBlur}
                      maxLength={50}
                      aria-describedby={hasError ? 'create-username-error' : undefined}
                      aria-invalid={hasError}
                    />
                    {hasError && (
                      <p id="create-username-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                        {String(errors[0] ?? fieldServerErrors.username)}
                      </p>
                    )}
                  </div>
                );
              }}
            </form.Field>

            {/* Email */}
            <form.Field
              name="email"
              validators={{
                onBlur: ({ value }) => validateField('email', value),
              }}
            >
              {(field) => {
                const errors = field.state.meta.errors;
                const hasError = errors.length > 0 || !!fieldServerErrors.email;
                return (
                  <div className={styles['fieldGroup']}>
                    <label htmlFor="create-email" className={styles['label']}>
                      Email
                    </label>
                    <input
                      id="create-email"
                      type="email"
                      className={styles['input']}
                      placeholder="user@example.com"
                      value={field.state.value}
                      onChange={(e) => field.handleChange(e.target.value)}
                      onBlur={field.handleBlur}
                      maxLength={254}
                      aria-describedby={hasError ? 'create-email-error' : undefined}
                      aria-invalid={hasError}
                    />
                    {hasError && (
                      <p id="create-email-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                        {String(errors[0] ?? fieldServerErrors.email)}
                      </p>
                    )}
                  </div>
                );
              }}
            </form.Field>

            {/* Role */}
            <form.Field
              name="role"
              validators={{
                onBlur: ({ value }) => validateField('role', value),
              }}
            >
              {(field) => {
                const errors = field.state.meta.errors;
                const hasError = errors.length > 0 || !!fieldServerErrors.role;
                return (
                  <div className={styles['fieldGroup']}>
                    <label htmlFor="create-role" className={styles['label']}>
                      Role
                    </label>
                    <select
                      id="create-role"
                      className={styles['select']}
                      value={field.state.value}
                      onChange={(e) => field.handleChange(e.target.value)}
                      onBlur={field.handleBlur}
                      aria-describedby={hasError ? 'create-role-error' : undefined}
                      aria-invalid={hasError}
                    >
                      {ROLE_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                    {hasError && (
                      <p id="create-role-error" className={styles['fieldError']} role="alert" aria-live="assertive">
                        {String(errors[0] ?? fieldServerErrors.role)}
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
                      disabled={!formValid || createUserMutation.isPending}
                      aria-busy={createUserMutation.isPending}
                    >
                      {createUserMutation.isPending && (
                        <span className={styles['spinner']} aria-hidden="true" />
                      )}
                      {createUserMutation.isPending ? 'Creating...' : 'Create'}
                    </button>
                  </div>
                );
              }}
            </form.Subscribe>
          </form>
        </div>
      )}

      {/* Users DataTable */}
      <DataTable<User>
        columns={columns}
        queryKey={queryKey}
        endpoint="/admin/users"
        defaultPageSize={search.size}
        emptyMessage="No users found."
      />

      {/* Confirmation Dialog */}
      <ConfirmDialog
        open={confirmDialog.open}
        title={confirmDialog.title}
        message={confirmDialog.message}
        confirmLabel={confirmDialog.confirmLabel}
        onConfirm={confirmDialog.onConfirm}
        onCancel={() => setConfirmDialog((prev) => ({ ...prev, open: false }))}
      />
    </div>
  );
}
