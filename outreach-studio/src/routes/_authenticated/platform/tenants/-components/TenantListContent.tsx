/**
 * Tenant List Content (lazy-loaded)
 *
 * Platform console: every organization with its status and member count, a search box, and a
 * form to create one. Creating an organization emails its first admin an activation link.
 */

import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useToast } from '@/hooks/useToast';
import type { NormalizedError, PageResponse } from '@/types/api';
import type { TenantSummary } from '@/types/tenant';

import styles from './PlatformTenants.module.css';

const PAGE_SIZE = 20;

interface CreateForm {
  name: string;
  slug: string;
  adminDisplayName: string;
  adminUsername: string;
  adminEmail: string;
}

const EMPTY: CreateForm = { name: '', slug: '', adminDisplayName: '', adminUsername: '', adminEmail: '' };

/** "Acme Volunteers India" → "acme-volunteers-india" */
export function slugify(name: string): string {
  // NFKD splits accented letters into letter + mark; dropping the marks keeps "Café" as "cafe".
  return name.normalize('NFKD').replace(/\p{M}/gu, '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 50);
}

function validate(form: CreateForm): Partial<Record<keyof CreateForm, string>> {
  const errors: Partial<Record<keyof CreateForm, string>> = {};
  if (form.name.trim().length < 3) errors.name = 'At least 3 characters';
  if (!/^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$/.test(form.slug)) errors.slug = 'Lower-case letters, digits and hyphens';
  if (!form.adminDisplayName.trim()) errors.adminDisplayName = "Enter the admin's name";
  if (!/^[a-zA-Z0-9._-]{3,50}$/.test(form.adminUsername)) errors.adminUsername = '3–50 letters, digits, . _ or -';
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.adminEmail)) errors.adminEmail = 'Enter a valid email address';
  return errors;
}

function CreateTenantForm({ onDone }: { onDone: () => void }) {
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();
  const [form, setForm] = useState<CreateForm>(EMPTY);
  const [slugTouched, setSlugTouched] = useState(false);
  const [errors, setErrors] = useState<Partial<Record<keyof CreateForm, string>>>({});

  const create = useMutation({
    mutationFn: async () => (await httpClient.post('/tenants', form)).data,
    onSuccess: () => {
      toastSuccess(`${form.name} created. ${form.adminDisplayName} has been emailed an invitation.`);
      void queryClient.invalidateQueries({ queryKey: queryKeys.tenants.all });
      onDone();
    },
    onError: (error: NormalizedError) => {
      setErrors(Object.fromEntries(error.fieldErrors.map((fe) => [fe.field, fe.message])));
      toastError(error.message || 'The organization could not be created');
    },
  });

  const set = (key: keyof CreateForm) => (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setForm((f) => ({
      ...f,
      [key]: value,
      ...(key === 'name' && !slugTouched ? { slug: slugify(value) } : {}),
    }));
    if (key === 'slug') setSlugTouched(true);
  };

  const field = (key: keyof CreateForm, label: string, props: React.InputHTMLAttributes<HTMLInputElement> = {}) => (
    <div className={styles['field']}>
      <label htmlFor={`tenant-${key}`} className={styles['label']}>{label}</label>
      <input id={`tenant-${key}`} className={styles['input']} value={form[key]} onChange={set(key)}
        aria-invalid={!!errors[key]} aria-describedby={errors[key] ? `tenant-${key}-error` : undefined} {...props} />
      {errors[key] && <p id={`tenant-${key}-error`} className={styles['fieldError']} role="alert">{errors[key]}</p>}
    </div>
  );

  return (
    <section className={styles['card']} aria-labelledby="create-heading">
      <h2 id="create-heading" className={styles['cardTitle']}>New organization</h2>
      <form
        className={styles['form']}
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          const problems = validate(form);
          setErrors(problems);
          if (Object.keys(problems).length === 0) create.mutate();
        }}
      >
        <fieldset className={styles['fieldset']}>
          <legend className={styles['legend']}>Organization</legend>
          {field('name', 'Name', { maxLength: 100 })}
          {field('slug', 'Short name', { maxLength: 50 })}
        </fieldset>
        <fieldset className={styles['fieldset']}>
          <legend className={styles['legend']}>First administrator (invited by email)</legend>
          {field('adminDisplayName', 'Full name', { maxLength: 100 })}
          {field('adminUsername', 'Username', { maxLength: 50 })}
          {field('adminEmail', 'Email', { type: 'email', maxLength: 254 })}
        </fieldset>
        <div className={styles['formActions']}>
          <button type="submit" className={styles['primaryBtn']} disabled={create.isPending}>
            {create.isPending ? 'Creating…' : 'Create organization'}
          </button>
          <button type="button" className={styles['secondaryBtn']} onClick={onDone}>Cancel</button>
        </div>
      </form>
    </section>
  );
}

export function TenantListContent() {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);

  const params = { search: search.trim() || undefined, page, size: PAGE_SIZE };
  const { data, isLoading, isError } = useQuery({
    queryKey: queryKeys.tenants.list(params),
    queryFn: async () => (await httpClient.get<PageResponse<TenantSummary>>('/tenants', { params })).data,
  });

  return (
    <div className={styles['container']}>
      <div className={styles['header']}>
        <div>
          <h1 className={styles['pageTitle']}>Organizations</h1>
          <p className={styles['subtle']}>Every organization on the platform. Suspended organizations can't sign in.</p>
        </div>
        {!creating && (
          <button type="button" className={styles['primaryBtn']} onClick={() => setCreating(true)}>
            New organization
          </button>
        )}
      </div>

      {creating && <CreateTenantForm onDone={() => setCreating(false)} />}

      <input
        type="search"
        className={`${styles['input']} ${styles['search']}`}
        placeholder="Search by name or short name"
        aria-label="Search organizations"
        value={search}
        onChange={(e) => { setSearch(e.target.value); setPage(0); }}
      />

      {isLoading && <p role="status">Loading organizations…</p>}
      {isError && <p role="alert">Organizations could not be loaded.</p>}
      {data && (
        <>
          <div className={styles['tableWrap']}>
            <table className={styles['table']} aria-label="Organizations">
              <thead>
                <tr>
                  <th scope="col">Name</th>
                  <th scope="col">Short name</th>
                  <th scope="col">Status</th>
                  <th scope="col">Members</th>
                  <th scope="col">Created</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((t) => (
                  <tr key={t.id}>
                    <td>
                      <Link to="/platform/tenants/$tenantId" params={{ tenantId: t.id }} className={styles['link']}>
                        {t.name}
                      </Link>
                    </td>
                    <td>{t.slug}</td>
                    <td><span className={`${styles['badge']} ${styles[`badge--${t.status.toLowerCase()}`]}`}>{t.status}</span></td>
                    <td>{t.memberCount}</td>
                    <td>{new Date(t.createdDate).toLocaleDateString()}</td>
                  </tr>
                ))}
                {data.content.length === 0 && (
                  <tr><td colSpan={5} className={styles['subtle']}>No organizations match.</td></tr>
                )}
              </tbody>
            </table>
          </div>
          {data.totalPages > 1 && (
            <div className={styles['pager']}>
              <button type="button" className={styles['secondaryBtn']} disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}>Previous</button>
              <span>Page {page + 1} of {data.totalPages}</span>
              <button type="button" className={styles['secondaryBtn']} disabled={page + 1 >= data.totalPages}
                onClick={() => setPage((p) => p + 1)}>Next</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
