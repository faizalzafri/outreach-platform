/**
 * Security Policy Content (lazy-loaded)
 *
 * The organization's sign-in security, from auth-service (/api/auth/security-policy):
 * - Password minimum length and how many previous passwords cannot be reused (never below the
 *   platform's own floor)
 * - One-time codes per action (sign-in, password reset, password change): on/off, code length,
 *   lifetime, attempts and resend wait
 */

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { useToast } from '@/hooks/useToast';
import type { NormalizedError } from '@/types/api';
import type { OtpPurpose, OtpSettings, SecurityPolicy } from '@/types/domain';

import styles from './SecurityPolicyContent.module.css';

const policyKey = (tenantId?: string) => ['admin', 'security-policy', tenantId ?? 'own'] as const;

/** Adds ?tenantId= when a platform admin works on another organization. */
const scopeFor = (tenantId?: string) => (tenantId ? { params: { tenantId } } : undefined);

const PURPOSES: { purpose: OtpPurpose; label: string; hint: string }[] = [
  { purpose: 'LOGIN', label: 'Sign-in', hint: 'After the password, before entering the app' },
  { purpose: 'PASSWORD_RESET', label: 'Password reset', hint: 'In addition to the emailed reset link' },
  { purpose: 'PASSWORD_CHANGE', label: 'Password change', hint: 'When a signed-in user changes their password' },
];

function PolicyForm({ initial, tenantId }: { initial: SecurityPolicy; tenantId?: string }) {
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();
  const [policy, setPolicy] = useState<SecurityPolicy>(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const minLength = initial.platformMinLength ?? 12;
  const minHistory = initial.platformHistoryCount ?? 0;

  const save = useMutation({
    mutationFn: async () => (await httpClient.put<SecurityPolicy>('/auth/security-policy', {
      passwordMinLength: policy.passwordMinLength,
      passwordHistoryCount: policy.passwordHistoryCount,
      otp: policy.otp,
    }, scopeFor(tenantId))).data,
    onSuccess: (saved) => {
      queryClient.setQueryData(policyKey(tenantId), saved);
      setErrors({});
      toastSuccess('Security policy saved. It applies from each user\'s next sign-in or password change.');
    },
    onError: (error: NormalizedError) => {
      setErrors(Object.fromEntries(error.fieldErrors.map((fe) => [fe.field, fe.message])));
      toastError(error.message || 'The policy could not be saved');
    },
  });

  const setOtp = (purpose: OtpPurpose, patch: Partial<OtpSettings>) =>
    setPolicy((p) => ({ ...p, otp: { ...p.otp, [purpose]: { ...p.otp[purpose], ...patch } } }));

  const number = (value: string) => (value === '' ? 0 : Number(value));

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save.mutate();
      }}
      noValidate
      className={styles['stack']}
    >
      <section className={styles['card']} aria-labelledby="passwords-heading">
        <h2 id="passwords-heading" className={styles['cardTitle']}>Passwords</h2>
        <p className={styles['hint']}>
          Every password also needs upper- and lower-case letters, a number and a symbol, and can't be a common word,
          the user's name or email. You can make these rules stricter than the platform's, not looser.
        </p>
        <div className={styles['row']}>
          <div className={styles['field']}>
            <label htmlFor="pw-min" className={styles['label']}>Minimum length</label>
            <input id="pw-min" className={styles['input']} type="number" min={minLength} max={128}
              value={policy.passwordMinLength}
              onChange={(e) => setPolicy((p) => ({ ...p, passwordMinLength: number(e.target.value) }))}
              aria-describedby="pw-min-hint" />
            <span id="pw-min-hint" className={styles['hint']}>Platform minimum: {minLength}</span>
            {errors['passwordMinLength'] && <p className={styles['error']} role="alert">{errors['passwordMinLength']}</p>}
          </div>
          <div className={styles['field']}>
            <label htmlFor="pw-history" className={styles['label']}>Recent passwords that can't be reused</label>
            <input id="pw-history" className={styles['input']} type="number" min={minHistory} max={24}
              value={policy.passwordHistoryCount}
              onChange={(e) => setPolicy((p) => ({ ...p, passwordHistoryCount: number(e.target.value) }))}
              aria-describedby="pw-history-hint" />
            <span id="pw-history-hint" className={styles['hint']}>Platform minimum: {minHistory}</span>
          </div>
        </div>
      </section>

      <section className={styles['card']} aria-labelledby="otp-heading">
        <h2 id="otp-heading" className={styles['cardTitle']}>One-time codes</h2>
        <p className={styles['hint']}>
          Ask for a code sent to the user's email for these actions. Users without an email address can't complete
          an action that requires a code.
        </p>
        <div className={styles['tableWrap']}>
          <table className={styles['table']}>
            <thead>
              <tr>
                <th scope="col">Action</th>
                <th scope="col">Digits</th>
                <th scope="col">Expires after (min)</th>
                <th scope="col">Attempts</th>
                <th scope="col">Resend wait (s)</th>
              </tr>
            </thead>
            <tbody>
              {PURPOSES.map(({ purpose, label, hint }) => {
                const s = policy.otp[purpose];
                return (
                  <tr key={purpose}>
                    <td>
                      <label className={styles['toggle']}>
                        <input type="checkbox" checked={s.enabled}
                          onChange={(e) => setOtp(purpose, { enabled: e.target.checked })} />
                        {label}
                      </label>
                      <div className={styles['rowHint']}>{hint}</div>
                    </td>
                    <td>
                      <select className={styles['input']} value={s.length} disabled={!s.enabled}
                        aria-label={`${label}: digits`} onChange={(e) => setOtp(purpose, { length: Number(e.target.value) })}>
                        {[6, 7, 8].map((n) => <option key={n} value={n}>{n}</option>)}
                      </select>
                    </td>
                    <td>
                      <input className={styles['input']} type="number" min={1} max={15} disabled={!s.enabled}
                        aria-label={`${label}: expires after minutes`} value={Math.round(s.ttlSeconds / 60)}
                        onChange={(e) => setOtp(purpose, { ttlSeconds: number(e.target.value) * 60 })} />
                    </td>
                    <td>
                      <input className={styles['input']} type="number" min={1} max={10} disabled={!s.enabled}
                        aria-label={`${label}: attempts`} value={s.maxAttempts}
                        onChange={(e) => setOtp(purpose, { maxAttempts: number(e.target.value) })} />
                    </td>
                    <td>
                      <input className={styles['input']} type="number" min={0} max={300} disabled={!s.enabled}
                        aria-label={`${label}: resend wait seconds`} value={s.resendCooldownSeconds}
                        onChange={(e) => setOtp(purpose, { resendCooldownSeconds: number(e.target.value) })} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {Object.keys(errors).some((k) => k.startsWith('otp')) && (
          <p className={styles['error']} role="alert">
            Check the code settings: 6–8 digits, 1–15 minutes, 1–10 attempts, and a resend wait of at most 300 seconds.
          </p>
        )}
      </section>

      <div className={styles['actions']}>
        <button type="submit" className={styles['primaryBtn']} disabled={save.isPending}>
          {save.isPending ? 'Saving…' : 'Save policy'}
        </button>
      </div>
    </form>
  );
}

export function SecurityPolicyContent({ tenantId, embedded = false }: { tenantId?: string; embedded?: boolean } = {}) {
  const { data, isLoading, isError } = useQuery({
    queryKey: policyKey(tenantId),
    queryFn: async () => (await httpClient.get<SecurityPolicy>('/auth/security-policy', scopeFor(tenantId))).data,
  });

  return (
    <div className={styles['container']}>
      {embedded
        ? <h2 className={styles['cardTitle']}>Sign-in security</h2>
        : <h1 className={styles['pageTitle']}>Sign-in security</h1>}
      {isLoading && <p role="status">Loading your organization's policy…</p>}
      {isError && <p role="alert">The policy could not be loaded. Please refresh the page.</p>}
      {data && <PolicyForm initial={data} tenantId={tenantId} />}
    </div>
  );
}
