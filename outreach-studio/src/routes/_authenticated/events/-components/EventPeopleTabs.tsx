/**
 * The people around an event: its POCs and the beneficiaries it serves. Shown to managers
 * (PMO and admins), who are the ones allowed to change them.
 */

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import type { NormalizedError, PageResponse } from '@/types/api';
import type { AssignmentRole, Beneficiary, PocAssignment, User } from '@/types/domain';

import styles from './EventDetailContent.module.css';

// ---------------------------------------------------------------------------
// POCs
// ---------------------------------------------------------------------------

export function PocsTab({ eventId }: { eventId: string }) {
  const queryClient = useQueryClient();
  const [userId, setUserId] = useState('');
  const [role, setRole] = useState<AssignmentRole>('PRIMARY');
  const [error, setError] = useState<string | null>(null);
  const pocsKey = [...queryKeys.events.detail(eventId), 'pocs'];

  const { data: pocs, isLoading } = useQuery<PocAssignment[]>({
    queryKey: pocsKey,
    queryFn: async () => (await httpClient.get<PocAssignment[]>(`/events/${eventId}/pocs`)).data,
  });

  // ponytail: first 200 POC users; a search box when tenants have more
  const { data: candidates } = useQuery<PageResponse<User>>({
    queryKey: queryKeys.admin.users({ role: 'ROLE_POC', page: 0, size: 200 }),
    queryFn: async () =>
      (await httpClient.get<PageResponse<User>>('/admin/users', { params: { role: 'ROLE_POC', size: 200 } })).data,
  });

  const onChanged = {
    onSuccess: () => {
      setError(null);
      void queryClient.invalidateQueries({ queryKey: pocsKey });
    },
    onError: (err: NormalizedError) => setError(err.message),
  };
  const assign = useMutation<unknown, NormalizedError, void>({
    mutationFn: () => httpClient.post(`/events/${eventId}/pocs`, { userId, role }),
    ...onChanged,
    onSuccess: () => {
      setUserId('');
      onChanged.onSuccess();
    },
  });
  const remove = useMutation<unknown, NormalizedError, string>({
    mutationFn: (pocUserId) => httpClient.delete(`/events/${eventId}/pocs/${pocUserId}`),
    ...onChanged,
  });

  const assigned = new Set(pocs?.map((p) => p.userId));
  const available = candidates?.content.filter((u) => !assigned.has(u.id)) ?? [];

  return (
    <div>
      <form
        className={styles['enrollForm']}
        onSubmit={(e) => {
          e.preventDefault();
          if (userId) assign.mutate();
        }}
      >
        <select
          className={styles['enrollInput']}
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          aria-label="POC to assign"
        >
          <option value="">Choose a POC…</option>
          {available.map((u) => (
            <option key={u.id} value={u.id}>
              {u.displayName ?? u.username}
            </option>
          ))}
        </select>
        <select
          className={styles['enrollInput']}
          value={role}
          onChange={(e) => setRole(e.target.value as AssignmentRole)}
          aria-label="Assignment role"
        >
          <option value="PRIMARY">Primary</option>
          <option value="SECONDARY">Secondary</option>
        </select>
        <button type="submit" className={styles['enrollBtn']} disabled={!userId || assign.isPending}>
          {assign.isPending ? 'Assigning...' : 'Assign'}
        </button>
      </form>
      {candidates && available.length === 0 && (
        <p className={styles['tabPlaceholder']}>
          Every POC in this organization is already assigned. Invite more from Administration.
        </p>
      )}
      {error && (
        <p className={styles['enrollError']} role="alert">
          {error}
        </p>
      )}

      {isLoading && <p className={styles['tabPlaceholder']}>Loading POCs...</p>}
      {pocs && pocs.length > 0 ? (
        <table className={styles['volunteerTable']}>
          <thead>
            <tr>
              <th scope="col">POC</th>
              <th scope="col">Role</th>
              <th scope="col">Assigned</th>
              <th scope="col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {pocs.map((poc) => (
              <tr key={poc.id}>
                <td>{poc.username}</td>
                <td>{poc.assignmentRole === 'PRIMARY' ? 'Primary' : 'Secondary'}</td>
                <td>{new Date(poc.assignedAt).toLocaleDateString()}</td>
                <td>
                  <button
                    type="button"
                    className={styles['enrollBtn']}
                    disabled={remove.isPending}
                    onClick={() => remove.mutate(poc.userId)}
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        !isLoading && <p className={styles['tabPlaceholder']}>No POCs assigned yet.</p>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Beneficiaries
// ---------------------------------------------------------------------------

export function BeneficiariesTab({ eventId }: { eventId: string }) {
  const queryClient = useQueryClient();
  const [beneficiaryId, setBeneficiaryId] = useState('');
  const [newName, setNewName] = useState('');
  const [newCity, setNewCity] = useState('');
  const [error, setError] = useState<string | null>(null);
  const linkedKey = [...queryKeys.events.detail(eventId), 'beneficiaries'];
  const allKey = ['beneficiaries', 'all'];

  const { data: linked, isLoading } = useQuery<Beneficiary[]>({
    queryKey: linkedKey,
    queryFn: async () => (await httpClient.get<Beneficiary[]>(`/events/${eventId}/beneficiaries`)).data,
  });

  // ponytail: first 200 beneficiaries; a search box when tenants have more
  const { data: all } = useQuery<PageResponse<Beneficiary>>({
    queryKey: allKey,
    queryFn: async () =>
      (await httpClient.get<PageResponse<Beneficiary>>('/beneficiaries', { params: { size: 200 } })).data,
  });

  const onError = (err: NormalizedError) => setError(err.message);
  const link = useMutation<Beneficiary[], NormalizedError, string>({
    mutationFn: async (id) =>
      (await httpClient.put<Beneficiary[]>(`/events/${eventId}/beneficiaries/${id}`)).data,
    onSuccess: (beneficiaries) => {
      setError(null);
      setBeneficiaryId('');
      queryClient.setQueryData(linkedKey, beneficiaries);
    },
    onError,
  });
  const unlink = useMutation<unknown, NormalizedError, string>({
    mutationFn: (id) => httpClient.delete(`/events/${eventId}/beneficiaries/${id}`),
    onSuccess: () => {
      setError(null);
      void queryClient.invalidateQueries({ queryKey: linkedKey });
    },
    onError,
  });
  // A beneficiary not in the list yet is created and linked in one go.
  const create = useMutation<Beneficiary, NormalizedError, void>({
    mutationFn: async () =>
      (
        await httpClient.post<Beneficiary>('/beneficiaries', {
          name: newName.trim(),
          city: newCity.trim() || null,
        })
      ).data,
    onSuccess: (created) => {
      setNewName('');
      setNewCity('');
      void queryClient.invalidateQueries({ queryKey: allKey });
      link.mutate(created.id);
    },
    onError,
  });

  const linkedIds = new Set(linked?.map((b) => b.id));
  const available = all?.content.filter((b) => b.active && !linkedIds.has(b.id)) ?? [];

  return (
    <div>
      <form
        className={styles['enrollForm']}
        onSubmit={(e) => {
          e.preventDefault();
          if (beneficiaryId) link.mutate(beneficiaryId);
        }}
      >
        <select
          className={styles['enrollInput']}
          value={beneficiaryId}
          onChange={(e) => setBeneficiaryId(e.target.value)}
          aria-label="Beneficiary to link"
        >
          <option value="">Choose a beneficiary…</option>
          {available.map((b) => (
            <option key={b.id} value={b.id}>
              {b.name}
              {b.city ? ` (${b.city})` : ''}
            </option>
          ))}
        </select>
        <button type="submit" className={styles['enrollBtn']} disabled={!beneficiaryId || link.isPending}>
          Link
        </button>
      </form>

      <form
        className={styles['enrollForm']}
        onSubmit={(e) => {
          e.preventDefault();
          if (newName.trim()) create.mutate();
        }}
      >
        <input
          className={styles['enrollInput']}
          placeholder="New beneficiary name"
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          aria-label="New beneficiary name"
        />
        <input
          className={styles['enrollInput']}
          placeholder="City (optional)"
          value={newCity}
          onChange={(e) => setNewCity(e.target.value)}
          aria-label="New beneficiary city"
        />
        <button type="submit" className={styles['enrollBtn']} disabled={!newName.trim() || create.isPending}>
          Add and link
        </button>
      </form>

      {error && (
        <p className={styles['enrollError']} role="alert">
          {error}
        </p>
      )}

      {isLoading && <p className={styles['tabPlaceholder']}>Loading beneficiaries...</p>}
      {linked && linked.length > 0 ? (
        <table className={styles['volunteerTable']}>
          <thead>
            <tr>
              <th scope="col">Beneficiary</th>
              <th scope="col">Organization</th>
              <th scope="col">City</th>
              <th scope="col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {linked.map((b) => (
              <tr key={b.id}>
                <td>{b.name}</td>
                <td>{b.organization ?? '—'}</td>
                <td>{b.city ?? '—'}</td>
                <td>
                  <button
                    type="button"
                    className={styles['enrollBtn']}
                    disabled={unlink.isPending}
                    onClick={() => unlink.mutate(b.id)}
                  >
                    Unlink
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        !isLoading && <p className={styles['tabPlaceholder']}>No beneficiaries linked yet.</p>
      )}
    </div>
  );
}
