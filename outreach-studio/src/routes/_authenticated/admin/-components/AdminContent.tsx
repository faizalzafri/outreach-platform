/**
 * Admin Content (lazy-loaded)
 *
 * User administration for the signed-in admin's organization, backed by auth-service
 * (/api/auth/users):
 * - Invite a user by email; they choose their own password from the activation link
 * - Status per user: invited, active, disabled, plus a locked flag after repeated failed sign-ins
 * - Resend or revoke a pending invitation
 * - Change role, disable/enable (enabling also clears a lockout), email a password reset
 */

import { useCallback, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { userInviteSchema, type UserInviteForm } from '@/lib/zod-schemas';
import { useAuth } from '@/hooks/useAuth';
import { useToast } from '@/hooks/useToast';
import { useFocusTrap } from '@/hooks/useFocusTrap';
import type { NormalizedError } from '@/types/api';
import type { Account, AccountRole } from '@/types/domain';

import styles from './AdminContent.module.css';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const ROLE_OPTIONS: { label: string; value: AccountRole }[] = [
  { label: 'Admin', value: 'ADMIN' },
  { label: 'PMO', value: 'PMO' },
  { label: 'POC', value: 'POC' },
];

/** Axios config adding ?tenantId= when a platform admin works on another organization. */
type TenantScope = { params: { tenantId: string } } | undefined;

const EMPTY_INVITE: UserInviteForm = { username: '', displayName: '', email: '', role: 'POC' };

function formatDate(iso: string | null): string {
  return iso ? new Date(iso).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '—';
}

function StatusBadges({ account }: { account: Account }) {
  return (
    <>
      <span className={`${styles['badge']} ${styles[`badge--${account.status.toLowerCase()}`]}`}>
        {account.status}
      </span>
      {account.locked && (
        <span className={`${styles['badge']} ${styles['badge--locked']}`} title="Locked after repeated failed sign-ins">
          Locked
        </span>
      )}
    </>
  );
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
  const dialogRef = useFocusTrap<HTMLDivElement>({ active: open, onEscape: onCancel });

  if (!open) return null;

  return (
    <div className={styles['dialogOverlay']} onClick={onCancel} role="presentation">
      <div
        ref={dialogRef}
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
// Invite form
// ---------------------------------------------------------------------------

function InviteForm({ onInvited, scope }: { onInvited: () => void; scope: TenantScope }) {
  const { success: toastSuccess, error: toastError } = useToast();
  const [values, setValues] = useState<UserInviteForm>(EMPTY_INVITE);
  const [errors, setErrors] = useState<Partial<Record<keyof UserInviteForm, string>>>({});

  const invite = useMutation({
    mutationFn: async (body: UserInviteForm) => (await httpClient.post<Account>('/auth/users', body, scope)).data,
    onSuccess: (account) => {
      toastSuccess(`Invitation sent to ${account.email}`);
      setValues(EMPTY_INVITE);
      onInvited();
    },
    onError: (error: NormalizedError) => {
      if (error.fieldErrors.length > 0) {
        setErrors(Object.fromEntries(error.fieldErrors.map((fe) => [fe.field, fe.message])));
      } else {
        toastError(error.message || 'Could not send the invitation');
      }
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const parsed = userInviteSchema.safeParse(values);
    if (!parsed.success) {
      setErrors(Object.fromEntries(parsed.error.issues.map((issue) => [issue.path[0], issue.message])));
      return;
    }
    setErrors({});
    invite.mutate(parsed.data);
  };

  const field = (name: keyof UserInviteForm, label: string, props: React.InputHTMLAttributes<HTMLInputElement>) => (
    <div className={styles['fieldGroup']}>
      <label htmlFor={`invite-${name}`} className={styles['label']}>{label}</label>
      <input
        id={`invite-${name}`}
        className={styles['input']}
        value={values[name]}
        onChange={(e) => setValues((v) => ({ ...v, [name]: e.target.value }))}
        aria-invalid={!!errors[name]}
        aria-describedby={errors[name] ? `invite-${name}-error` : undefined}
        {...props}
      />
      {errors[name] && (
        <p id={`invite-${name}-error`} className={styles['fieldError']} role="alert">{errors[name]}</p>
      )}
    </div>
  );

  return (
    <div className={styles['createSection']}>
      <h2 className={styles['createSectionTitle']}>Invite a user</h2>
      <p className={styles['hint']}>
        They'll get an email with a link to set their own password. The link works once and expires after 72 hours.
      </p>
      <form className={styles['createForm']} onSubmit={submit} noValidate>
        {field('displayName', 'Full name', { maxLength: 100, autoComplete: 'off' })}
        {field('username', 'Username', { maxLength: 50, autoComplete: 'off', placeholder: 'e.g. meera.iyer' })}
        {field('email', 'Email', { type: 'email', maxLength: 254, autoComplete: 'off' })}
        <div className={styles['fieldGroup']}>
          <label htmlFor="invite-role" className={styles['label']}>Role</label>
          <select
            id="invite-role"
            className={styles['select']}
            value={values.role}
            onChange={(e) => setValues((v) => ({ ...v, role: e.target.value as AccountRole }))}
          >
            {ROLE_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
          </select>
        </div>
        <div className={styles['formActions']}>
          <button type="submit" className={styles['submitBtn']} disabled={invite.isPending} aria-busy={invite.isPending}>
            {invite.isPending && <span className={styles['spinner']} aria-hidden="true" />}
            {invite.isPending ? 'Sending…' : 'Send invitation'}
          </button>
        </div>
      </form>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

/** Optional tenant scope: platform admins manage another organization's users through the same page. */
export function AdminContent({ tenantId, embedded = false }: { tenantId?: string; embedded?: boolean } = {}) {
  const scope: TenantScope = tenantId ? { params: { tenantId } } : undefined;
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const { success: toastSuccess, error: toastError } = useToast();
  const [showInvite, setShowInvite] = useState(false);
  const [confirm, setConfirm] = useState<Omit<ConfirmDialogProps, 'onCancel'>>({
    open: false, title: '', message: '', confirmLabel: '', onConfirm: () => {},
  });

  const accountsKey = queryKeys.admin.accounts(tenantId);
  const { data: accounts, isLoading, isError, refetch } = useQuery({
    queryKey: accountsKey,
    queryFn: async () => (await httpClient.get<Account[]>('/auth/users', scope)).data,
  });

  const refresh = useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.admin.all });
  }, [queryClient]);

  /** One mutation for every row action; each action supplies its request and success message. */
  const action = useMutation({
    mutationFn: async ({ request }: { request: () => Promise<unknown>; done: string }) => request(),
    onSuccess: (_data, { done }) => {
      toastSuccess(done);
      refresh();
    },
    onError: (error: NormalizedError) => toastError(error.message || 'The change could not be saved'),
  });

  const run = (request: () => Promise<unknown>, done: string) => action.mutate({ request, done });

  const ask = (title: string, message: string, confirmLabel: string, onConfirm: () => void) =>
    setConfirm({
      open: true, title, message, confirmLabel,
      onConfirm: () => {
        onConfirm();
        setConfirm((c) => ({ ...c, open: false }));
      },
    });

  const rowActions = (account: Account) => {
    const self = account.username === user?.sub;
    const base = `/auth/users/${account.id}`;
    const buttons: React.ReactNode[] = [];

    if (account.status === 'INVITED') {
      buttons.push(
        <button key="resend" type="button" className={styles['actionBtn']}
          onClick={() => run(() => httpClient.post(`${base}/invitation`, null, scope), `Invitation re-sent to ${account.email}`)}>
          Resend invite
        </button>,
        <button key="revoke" type="button" className={styles['actionBtnDanger']}
          onClick={() => ask('Revoke invitation',
            `${account.displayName}'s invitation link will stop working and they will be removed.`, 'Revoke',
            () => run(() => httpClient.delete(`${base}/invitation`, scope), 'Invitation revoked'))}>
          Revoke
        </button>,
      );
    }
    if (account.status === 'ACTIVE') {
      buttons.push(
        <button key="reset" type="button" className={styles['actionBtn']}
          onClick={() => ask('Send password reset',
            `Email ${account.displayName} a link to choose a new password? Their current password keeps working until they do.`,
            'Send link', () => run(() => httpClient.post(`${base}/password-reset`, null, scope), `Reset link sent to ${account.email}`))}>
          Send reset link
        </button>,
      );
      if (!self) {
        buttons.push(
          <button key="disable" type="button" className={styles['actionBtnDanger']}
            onClick={() => ask('Disable account', `${account.displayName} will no longer be able to sign in.`, 'Disable',
              () => run(() => httpClient.post(`${base}/disable`, null, scope), 'Account disabled'))}>
            Disable
          </button>,
        );
      }
    }
    if (account.status === 'DISABLED' || (account.status === 'ACTIVE' && account.locked)) {
      buttons.push(
        <button key="enable" type="button" className={styles['actionBtn']}
          onClick={() => run(() => httpClient.post(`${base}/enable`, null, scope),
            account.locked ? 'Account unlocked' : 'Account enabled')}>
          {account.status === 'DISABLED' ? 'Enable' : 'Unlock'}
        </button>,
      );
    }
    return <div className={styles['actions']}>{buttons}</div>;
  };

  return (
    <div className={styles['container']}>
      <div className={styles['header']}>
        {embedded
          ? <h2 className={styles['createSectionTitle']}>People</h2>
          : <h1 className={styles['pageTitle']}>User Administration</h1>}
        <button type="button" className={styles['toggleBtn']} onClick={() => setShowInvite((v) => !v)}>
          {showInvite ? 'Cancel' : 'Invite user'}
        </button>
      </div>

      {showInvite && <InviteForm scope={scope} onInvited={() => { setShowInvite(false); refresh(); }} />}

      {isLoading && <p className={styles['muted']} role="status">Loading users…</p>}
      {isError && (
        <div className={styles['serverError']} role="alert">
          Users could not be loaded. <button type="button" className={styles['actionBtn']} onClick={() => void refetch()}>Retry</button>
        </div>
      )}

      {accounts && (
        <div className={styles['tableWrap']}>
          <table className={styles['table']} aria-label="Users">
            <thead>
              <tr>
                <th scope="col">Name</th>
                <th scope="col">Email</th>
                <th scope="col">Role</th>
                <th scope="col">Status</th>
                <th scope="col">Last sign-in</th>
                <th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((account) => (
                <tr key={account.id}>
                  <td>
                    <div className={styles['userName']}>{account.displayName}</div>
                    <div className={styles['userHandle']}>{account.username}</div>
                  </td>
                  <td>{account.email}</td>
                  <td>
                    <select
                      className={styles['roleSelect']}
                      value={account.role}
                      disabled={account.username === user?.sub}
                      title={account.username === user?.sub ? 'You cannot change your own role' : undefined}
                      onChange={(e) => {
                        // Read now: the controlled select snaps back to the saved role before the request runs.
                        const role = e.target.value as AccountRole;
                        run(() => httpClient.put(`/auth/users/${account.id}/role`, { role }, scope),
                          `${account.displayName} is now ${role}`);
                      }}
                      aria-label={`Role for ${account.displayName}`}
                    >
                      {ROLE_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                    </select>
                  </td>
                  <td><StatusBadges account={account} /></td>
                  <td className={styles['muted']}>{formatDate(account.lastLoginAt)}</td>
                  <td>{rowActions(account)}</td>
                </tr>
              ))}
              {accounts.length === 0 && (
                <tr><td colSpan={6} className={styles['muted']}>No users yet. Invite someone to get started.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog {...confirm} onCancel={() => setConfirm((c) => ({ ...c, open: false }))} />
    </div>
  );
}
