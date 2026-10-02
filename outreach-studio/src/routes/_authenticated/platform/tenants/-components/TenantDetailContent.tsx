/**
 * Tenant Detail Content (lazy-loaded)
 *
 * Platform console, one organization: rename it, suspend / reactivate / deactivate it, and manage
 * its people and sign-in security with the same screens its own admins use.
 */

import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useToast } from '@/hooks/useToast';
import type { NormalizedError } from '@/types/api';
import type { TenantDetail, TenantStatus } from '@/types/tenant';
import { AdminContent } from '@/routes/_authenticated/admin/-components/AdminContent';
import { SecurityPolicyContent } from '@/routes/_authenticated/admin/security/-components/SecurityPolicyContent';

import styles from './PlatformTenants.module.css';

const STATUS_ACTIONS: { target: TenantStatus; path: string; label: string; confirm: string; danger: boolean }[] = [
  { target: 'ACTIVE', path: 'activate', label: 'Reactivate', confirm: 'Members will be able to sign in again.', danger: false },
  { target: 'SUSPENDED', path: 'suspend', label: 'Suspend', confirm: 'Members will be signed out within a minute and cannot sign in until you reactivate it.', danger: true },
  { target: 'DEACTIVATED', path: 'deactivate', label: 'Deactivate', confirm: 'Use this for organizations that have left. Their data is kept, but nobody can sign in.', danger: true },
];

export function TenantDetailContent({ tenantId }: { tenantId: string }) {
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();
  const [renaming, setRenaming] = useState(false);
  const [name, setName] = useState('');

  const key = [...queryKeys.tenants.all, 'detail', tenantId] as const;
  const { data: tenant, isLoading, isError } = useQuery({
    queryKey: key,
    queryFn: async () => (await httpClient.get<TenantDetail>(`/tenants/${tenantId}`)).data,
  });

  const update = useMutation({
    mutationFn: async (request: () => Promise<{ data: TenantDetail }>) => (await request()).data,
    onSuccess: (saved) => {
      queryClient.setQueryData(key, saved);
      void queryClient.invalidateQueries({ queryKey: queryKeys.tenants.all });
      setRenaming(false);
      toastSuccess('Organization updated');
    },
    onError: (error: NormalizedError) => toastError(error.message || 'The change could not be saved'),
  });

  if (isLoading) return <div className={styles['container']}><p role="status">Loading organization…</p></div>;
  if (isError || !tenant) return <div className={styles['container']}><p role="alert">Organization not found.</p></div>;

  const members = (role: 'ADMIN' | 'PMO' | 'POC') => tenant.membersByRole[role] ?? 0;

  return (
    <div className={styles['container']}>
      <Link to="/platform/tenants" className={styles['link']}>← All organizations</Link>

      <section className={styles['card']} aria-labelledby="tenant-heading">
        <div className={styles['header']}>
          {renaming ? (
            <form
              className={styles['statusActions']}
              onSubmit={(e) => {
                e.preventDefault();
                update.mutate(() => httpClient.put<TenantDetail>(`/tenants/${tenantId}`, { name }));
              }}
            >
              <input className={styles['input']} aria-label="Organization name" value={name} maxLength={100}
                onChange={(e) => setName(e.target.value)} autoFocus />
              <button type="submit" className={styles['primaryBtn']} disabled={name.trim().length < 3}>Save</button>
              <button type="button" className={styles['secondaryBtn']} onClick={() => setRenaming(false)}>Cancel</button>
            </form>
          ) : (
            <div>
              <h1 id="tenant-heading" className={styles['pageTitle']}>{tenant.name}</h1>
              <p className={styles['subtle']}>{tenant.slug}</p>
            </div>
          )}
          <span className={`${styles['badge']} ${styles[`badge--${tenant.status.toLowerCase()}`]}`}>{tenant.status}</span>
        </div>

        <dl className={styles['facts']}>
          <div><dt>Admins</dt><dd>{members('ADMIN')}</dd></div>
          <div><dt>PMOs</dt><dd>{members('PMO')}</dd></div>
          <div><dt>POCs</dt><dd>{members('POC')}</dd></div>
          <div><dt>Created</dt><dd>{new Date(tenant.createdDate).toLocaleDateString()}</dd></div>
          {tenant.lastModifiedBy && (
            <div><dt>Last changed by</dt><dd>{tenant.lastModifiedBy}</dd></div>
          )}
        </dl>

        <div className={`${styles['statusActions']} ${styles['spaced']}`}>
          {!renaming && (
            <button type="button" className={styles['secondaryBtn']}
              onClick={() => { setName(tenant.name); setRenaming(true); }}>Rename</button>
          )}
          {STATUS_ACTIONS.filter((a) => a.target !== tenant.status).map((a) => (
            <button key={a.path} type="button" className={a.danger ? styles['dangerBtn'] : styles['secondaryBtn']}
              disabled={update.isPending}
              onClick={() => {
                if (window.confirm(`${a.label} ${tenant.name}? ${a.confirm}`)) {
                  update.mutate(() => httpClient.post<TenantDetail>(`/tenants/${tenantId}/${a.path}`));
                }
              }}>
              {a.label}
            </button>
          ))}
        </div>
      </section>

      <section className={styles['card']}>
        <AdminContent tenantId={tenantId} embedded />
      </section>

      <section className={styles['card']}>
        <SecurityPolicyContent tenantId={tenantId} embedded />
      </section>
    </div>
  );
}
