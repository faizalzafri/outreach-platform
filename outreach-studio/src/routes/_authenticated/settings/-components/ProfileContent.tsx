/**
 * Profile Content (lazy-loaded)
 *
 * The signed-in user's own account, from auth-service (/api/auth/me):
 * - Account facts: username, email, role, last sign-in, password age
 * - Edit display name and phone
 * - Change password (current password required; a one-time code too when the organization asks)
 */

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useAuth } from '@/hooks/useAuth';
import { useToast } from '@/hooks/useToast';
import type { NormalizedError } from '@/types/api';
import type { Profile } from '@/types/domain';

import styles from './ProfileContent.module.css';

function formatDate(iso: string | null): string {
  return iso ? new Date(iso).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '—';
}

function fieldErrorMap(error: NormalizedError): Record<string, string> {
  return Object.fromEntries(error.fieldErrors.map((fe) => [fe.field, fe.message]));
}

// ---------------------------------------------------------------------------
// Details
// ---------------------------------------------------------------------------

function DetailsForm({ profile }: { profile: Profile }) {
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();
  const [displayName, setDisplayName] = useState(profile.displayName);
  const [phone, setPhone] = useState(profile.phone ?? '');
  const [errors, setErrors] = useState<Record<string, string>>({});

  const save = useMutation({
    mutationFn: async () => (await httpClient.put<Profile>('/auth/me', { displayName, phone })).data,
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.profile.me(), updated);
      setErrors({});
      toastSuccess('Profile saved. Your new name appears after your next sign-in.');
    },
    onError: (error: NormalizedError) => {
      setErrors(fieldErrorMap(error));
      if (error.fieldErrors.length === 0) toastError(error.message || 'Your profile could not be saved');
    },
  });

  return (
    <form
      className={styles['form']}
      onSubmit={(e) => {
        e.preventDefault();
        if (!displayName.trim()) {
          setErrors({ displayName: 'Enter your name' });
          return;
        }
        save.mutate();
      }}
      noValidate
    >
      <div className={styles['field']}>
        <label htmlFor="profile-name" className={styles['label']}>Display name</label>
        <input id="profile-name" className={styles['input']} value={displayName} maxLength={100}
          onChange={(e) => setDisplayName(e.target.value)} aria-invalid={!!errors['displayName']}
          aria-describedby={errors['displayName'] ? 'profile-name-error' : undefined} />
        {errors['displayName'] && <p id="profile-name-error" className={styles['fieldError']} role="alert">{errors['displayName']}</p>}
      </div>
      <div className={styles['field']}>
        <label htmlFor="profile-phone" className={styles['label']}>Phone (optional)</label>
        <input id="profile-phone" className={styles['input']} type="tel" value={phone} maxLength={20}
          autoComplete="tel" onChange={(e) => setPhone(e.target.value)} aria-invalid={!!errors['phone']}
          aria-describedby={errors['phone'] ? 'profile-phone-error' : undefined} />
        {errors['phone'] && <p id="profile-phone-error" className={styles['fieldError']} role="alert">{errors['phone']}</p>}
      </div>
      <div className={styles['actions']}>
        <button type="submit" className={styles['primaryBtn']} disabled={save.isPending}>
          {save.isPending ? 'Saving…' : 'Save changes'}
        </button>
      </div>
    </form>
  );
}

// ---------------------------------------------------------------------------
// Password
// ---------------------------------------------------------------------------

const EMPTY_PASSWORD = { currentPassword: '', newPassword: '', confirmPassword: '', otpCode: '' };

function PasswordForm({ minLength }: { minLength: number }) {
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();
  const [values, setValues] = useState(EMPTY_PASSWORD);
  const [errors, setErrors] = useState<Record<string, string>>({});
  // Set once the server says this change needs a one-time code (it has just sent one).
  const [codeSent, setCodeSent] = useState(false);

  const change = useMutation({
    mutationFn: async () => httpClient.post('/auth/me/password', {
      currentPassword: values.currentPassword,
      newPassword: values.newPassword,
      otpCode: codeSent ? values.otpCode : undefined,
    }),
    onSuccess: () => {
      setValues(EMPTY_PASSWORD);
      setErrors({});
      setCodeSent(false);
      void queryClient.invalidateQueries({ queryKey: queryKeys.profile.me() });
      toastSuccess('Password changed. We emailed you a confirmation.');
    },
    onError: (error: NormalizedError) => {
      if (error.type === 'OTP_REQUIRED') {
        setCodeSent(true);
        setErrors({});
        return;
      }
      setErrors(fieldErrorMap(error));
      if (error.fieldErrors.length === 0) toastError(error.message || 'Your password could not be changed');
    },
  });

  const set = (key: keyof typeof EMPTY_PASSWORD) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setValues((v) => ({ ...v, [key]: e.target.value }));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (values.newPassword !== values.confirmPassword) {
      setErrors({ confirmPassword: "The two passwords don't match" });
      return;
    }
    change.mutate();
  };

  const input = (key: keyof typeof EMPTY_PASSWORD, label: string, autoComplete: string, extra?: React.ReactNode) => (
    <div className={styles['field']}>
      <label htmlFor={`pw-${key}`} className={styles['label']}>{label}</label>
      <input id={`pw-${key}`} className={styles['input']} type={key === 'otpCode' ? 'text' : 'password'}
        inputMode={key === 'otpCode' ? 'numeric' : undefined} autoComplete={autoComplete}
        value={values[key]} onChange={set(key)} required aria-invalid={!!errors[key]}
        aria-describedby={errors[key] ? `pw-${key}-error` : undefined} />
      {extra}
      {errors[key] && <p id={`pw-${key}-error`} className={styles['fieldError']} role="alert">{errors[key]}</p>}
    </div>
  );

  return (
    <form className={styles['form']} onSubmit={submit} noValidate>
      {input('currentPassword', 'Current password', 'current-password')}
      {input('newPassword', 'New password', 'new-password',
        <ul className={styles['rules']}>
          <li>At least {minLength} characters, with upper- and lower-case letters, a number and a symbol</li>
          <li>Not a common word, your username, or one of your recent passwords</li>
        </ul>)}
      {input('confirmPassword', 'Confirm new password', 'new-password')}
      {codeSent && (
        <>
          <p className={styles['notice']} role="status">
            For your security we emailed you a verification code. Enter it to finish changing your password.
          </p>
          {input('otpCode', 'Verification code', 'one-time-code')}
        </>
      )}
      <div className={styles['actions']}>
        <button type="submit" className={styles['primaryBtn']} disabled={change.isPending}>
          {change.isPending ? 'Changing…' : 'Change password'}
        </button>
      </div>
    </form>
  );
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export function ProfileContent() {
  const { user } = useAuth();
  const { data: profile, isLoading, isError } = useQuery({
    queryKey: queryKeys.profile.me(),
    queryFn: async () => (await httpClient.get<Profile>('/auth/me')).data,
  });

  const role = user?.roles.find((r) => r.startsWith('ROLE_'))?.replace('ROLE_', '').replace('_', ' ') ?? '—';

  return (
    <div className={styles['container']}>
      <h1 className={styles['pageTitle']}>Your profile</h1>

      {isLoading && <p role="status">Loading your profile…</p>}
      {isError && <p role="alert">Your profile could not be loaded. Please refresh the page.</p>}

      {profile && (
        <>
          <section className={styles['card']} aria-labelledby="account-heading">
            <h2 id="account-heading" className={styles['cardTitle']}>Account</h2>
            <p className={styles['cardHint']}>Ask an administrator to change your username, email or role.</p>
            <dl className={styles['facts']}>
              <dt>Username</dt><dd>{profile.username}</dd>
              <dt>Email</dt><dd>{profile.email}</dd>
              <dt>Role</dt><dd>{profile.platformAdmin ? 'Platform admin' : role}</dd>
              <dt>Last sign-in</dt><dd>{formatDate(profile.lastLoginAt)}</dd>
              <dt>Password changed</dt><dd>{formatDate(profile.passwordChangedAt)}</dd>
            </dl>
          </section>

          <section className={styles['card']} aria-labelledby="details-heading">
            <h2 id="details-heading" className={styles['cardTitle']}>Personal details</h2>
            <p className={styles['cardHint']}>Your name is shown to colleagues across Outreach Studio.</p>
            {/* Keyed so the form starts from the saved values whenever they change */}
            <DetailsForm key={`${profile.displayName}|${profile.phone ?? ''}`} profile={profile} />
          </section>

          <section className={styles['card']} aria-labelledby="password-heading">
            <h2 id="password-heading" className={styles['cardTitle']}>Password</h2>
            <p className={styles['cardHint']}>Choose a password you don't use anywhere else.</p>
            <PasswordForm minLength={profile.passwordMinLength} />
          </section>
        </>
      )}
    </div>
  );
}
