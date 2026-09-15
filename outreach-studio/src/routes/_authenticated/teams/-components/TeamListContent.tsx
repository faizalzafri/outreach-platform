/**
 * Team List Content (lazy-loaded)
 *
 * Team management list with:
 * - Team list in DataTable (name, description, member count, created date, actions)
 * - Inline "Create Team" form using the shared TeamForm component
 * - Delete confirmation dialog
 * - Success/error toasts on mutations
 */

import { useMemo, useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import type { ColumnDef } from '@tanstack/react-table';

import { DataTable } from '@/components/data-table/DataTable';
import { TeamForm } from '@/components/teams/TeamForm';
import { queryKeys } from '@/lib/query-keys';
import { truncateName } from '@/lib/tenant-utils';
import { useToast } from '@/hooks/useToast';
import { useFocusTrap } from '@/hooks/useFocusTrap';
import { useEffectiveTenantId } from '@/hooks/useTenant';
import { useCreateTeam, useDeleteTeam } from '@/hooks/useTeams';
import type { NormalizedError } from '@/types/api';
import type { Team } from '@/types/tenant';
import type { TeamFormValues } from '@/lib/zod-schemas';

import { Route } from '../index';
import styles from './TeamListContent.module.css';

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
  const dialogRef = useFocusTrap<HTMLDivElement>({ active: open, onEscape: onCancel });

  if (!open) return null;

  return (
    <div className={styles['dialogOverlay']} onClick={onCancel} role="presentation">
      <div
        ref={dialogRef}
        className={styles['dialog']}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="team-confirm-dialog-title"
        aria-describedby="team-confirm-dialog-message"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="team-confirm-dialog-title" className={styles['dialogTitle']}>
          {title}
        </h2>
        <p id="team-confirm-dialog-message" className={styles['dialogMessage']}>
          {message}
        </p>
        <div className={styles['dialogActions']}>
          <button type="button" className={styles['dialogCancelBtn']} onClick={onCancel}>
            Cancel
          </button>
          <button type="button" className={styles['dialogConfirmBtn']} onClick={onConfirm}>
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

export function TeamListContent() {
  const search = Route.useSearch();
  const navigate = useNavigate();
  const tenantId = useEffectiveTenantId();
  const { success: toastSuccess, error: toastError } = useToast();

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [fieldServerErrors, setFieldServerErrors] = useState<Record<string, string>>({});
  const [confirmDialog, setConfirmDialog] = useState<{
    open: boolean;
    title: string;
    message: string;
    confirmLabel: string;
    onConfirm: () => void;
  }>({ open: false, title: '', message: '', confirmLabel: '', onConfirm: () => {} });

  const createTeamMutation = useCreateTeam();
  const deleteTeamMutation = useDeleteTeam();

  function handleCreate(values: TeamFormValues) {
    setCreateError(null);
    setFieldServerErrors({});
    createTeamMutation.mutate(values, {
      onSuccess: () => {
        toastSuccess('Team created successfully');
        setShowCreateForm(false);
      },
      onError: (error) => {
        const normalized = error as NormalizedError;
        if (normalized.fieldErrors && normalized.fieldErrors.length > 0) {
          const mapped: Record<string, string> = {};
          for (const fe of normalized.fieldErrors) {
            mapped[fe.field] = fe.message;
          }
          setFieldServerErrors(mapped);
        } else {
          setCreateError(normalized.message || 'Failed to create team');
          toastError(normalized.message || 'Failed to create team');
        }
      },
    });
  }

  function handleDelete(team: Team) {
    setConfirmDialog({
      open: true,
      title: 'Delete Team',
      message: `Deleting "${team.name}" will revoke any resource permissions granted to this team. This cannot be undone.`,
      confirmLabel: 'Delete',
      onConfirm: () => {
        deleteTeamMutation.mutate(team.id, {
          onSuccess: () => toastSuccess('Team deleted'),
          onError: (error) => {
            const normalized = error as NormalizedError;
            toastError(normalized.message || 'Failed to delete team');
          },
        });
        setConfirmDialog((prev) => ({ ...prev, open: false }));
      },
    });
  }

  const columns: ColumnDef<Team, unknown>[] = useMemo(
    () => [
      {
        accessorKey: 'name',
        header: 'Name',
        enableSorting: true,
        enableColumnFilter: false,
      },
      {
        accessorKey: 'description',
        header: 'Description',
        enableSorting: false,
        enableColumnFilter: false,
        cell: ({ row }) => truncateName(row.original.description, 100),
      },
      {
        accessorKey: 'memberCount',
        header: 'Members',
        enableSorting: true,
        enableColumnFilter: false,
      },
      {
        accessorKey: 'createdDate',
        header: 'Created',
        enableSorting: true,
        enableColumnFilter: false,
        cell: ({ row }) => new Date(row.original.createdDate).toLocaleDateString(),
      },
      {
        id: 'actions',
        header: 'Actions',
        enableSorting: false,
        enableColumnFilter: false,
        enableHiding: false,
        cell: ({ row }) => {
          const team = row.original;
          return (
            <div className={styles['actions']}>
              <button
                type="button"
                className={styles['actionBtn']}
                onClick={() => void navigate({ to: '/teams/$teamId', params: { teamId: team.id }, search: { edit: false } })}
              >
                View
              </button>
              <button
                type="button"
                className={styles['actionBtn']}
                onClick={() =>
                  void navigate({
                    to: '/teams/$teamId',
                    params: { teamId: team.id },
                    search: { edit: true },
                  })
                }
              >
                Edit
              </button>
              <button
                type="button"
                className={styles['actionBtnDanger']}
                onClick={() => handleDelete(team)}
              >
                Delete
              </button>
            </div>
          );
        },
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps -- navigate/handleDelete are stable enough for this table's lifetime
    [],
  );

  const queryKey = useMemo(
    () => queryKeys.teams.list(tenantId, { page: search.page, size: search.size }),
    [tenantId, search.page, search.size],
  );

  return (
    <div className={styles['container']}>
      <div className={styles['header']}>
        <h1 className={styles['pageTitle']}>Teams</h1>
        <button
          type="button"
          className={styles['toggleBtn']}
          onClick={() => setShowCreateForm((prev) => !prev)}
        >
          {showCreateForm ? 'Cancel' : 'Create Team'}
        </button>
      </div>

      {showCreateForm && (
        <div className={styles['createSection']}>
          <h2 className={styles['createSectionTitle']}>Create New Team</h2>
          {createError && (
            <div className={styles['serverError']} role="alert">
              {createError}
            </div>
          )}
          <TeamForm
            mode="create"
            onSubmit={handleCreate}
            onCancel={() => setShowCreateForm(false)}
            isSubmitting={createTeamMutation.isPending}
            fieldServerErrors={fieldServerErrors}
          />
        </div>
      )}

      <DataTable<Team>
        columns={columns}
        queryKey={queryKey}
        endpoint="/teams"
        defaultPageSize={search.size}
        emptyMessage="No teams found."
      />

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
