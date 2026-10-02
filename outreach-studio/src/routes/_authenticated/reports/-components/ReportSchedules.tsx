/**
 * Scheduled reports: the event-scores report emailed to recipients on a schedule.
 */

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import type { NormalizedError } from '@/types/api';

import styles from './ReportsContent.module.css';

type Format = 'PDF' | 'CSV' | 'EXCEL';

interface Schedule {
  id: string;
  name: string;
  cronExpression: string;
  exportFormat: Format;
  recipients: string[];
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED';
  nextRunAt: string | null;
}

// Spring cron: second minute hour day-of-month month day-of-week. All at 08:00 server time.
const FREQUENCIES = [
  { label: 'Every day', cron: '0 0 8 * * *' },
  { label: 'Every Monday', cron: '0 0 8 * * MON' },
  { label: 'First day of every month', cron: '0 0 8 1 * *' },
];
const frequencyLabel = (cron: string) => FREQUENCIES.find((f) => f.cron === cron)?.label ?? cron;

const SCHEDULES_KEY = ['reports', 'schedules'];

export function ReportSchedules() {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [cron, setCron] = useState(FREQUENCIES[1]!.cron);
  const [format, setFormat] = useState<Format>('PDF');
  const [recipients, setRecipients] = useState('');
  const [error, setError] = useState<string | null>(null);

  const { data: schedules } = useQuery<Schedule[]>({
    queryKey: SCHEDULES_KEY,
    queryFn: async () => (await httpClient.get<Schedule[]>('/reports/scheduled')).data,
  });

  const refresh = () => {
    setError(null);
    void queryClient.invalidateQueries({ queryKey: SCHEDULES_KEY });
  };
  const onError = (err: NormalizedError) => setError(err.message);

  const create = useMutation<unknown, NormalizedError, void>({
    mutationFn: () =>
      httpClient.post('/reports/scheduled', {
        name: name.trim(),
        reportType: 'BY_EVENT',
        cronExpression: cron,
        exportFormat: format,
        recipients: recipients.split(',').map((r) => r.trim()).filter(Boolean),
      }),
    onSuccess: () => {
      setName('');
      setRecipients('');
      refresh();
    },
    onError,
  });
  const setStatus = useMutation<unknown, NormalizedError, { id: string; status: Schedule['status'] }>({
    mutationFn: ({ id, status }) => httpClient.put(`/reports/scheduled/${id}`, { status }),
    onSuccess: refresh,
    onError,
  });
  const remove = useMutation<unknown, NormalizedError, string>({
    mutationFn: (id) => httpClient.delete(`/reports/scheduled/${id}`),
    onSuccess: refresh,
    onError,
  });

  return (
    <section className={styles['chartSection']}>
      <h2 className={styles['chartTitle']}>Scheduled reports</h2>
      <p>The event scores report, emailed as a file on a schedule (08:00 server time).</p>
      <form
        className={styles['filterRow']}
        onSubmit={(e) => {
          e.preventDefault();
          if (name.trim() && recipients.trim()) create.mutate();
        }}
      >
        <input aria-label="Schedule name" placeholder="Name, e.g. Weekly scores" value={name}
          onChange={(e) => setName(e.target.value)} />
        <select aria-label="Frequency" value={cron} onChange={(e) => setCron(e.target.value)}>
          {FREQUENCIES.map((f) => (
            <option key={f.cron} value={f.cron}>{f.label}</option>
          ))}
        </select>
        <select aria-label="Format" value={format} onChange={(e) => setFormat(e.target.value as Format)}>
          <option value="PDF">PDF</option>
          <option value="EXCEL">Excel</option>
          <option value="CSV">CSV</option>
        </select>
        <input aria-label="Recipients" placeholder="Emails, comma-separated" value={recipients}
          onChange={(e) => setRecipients(e.target.value)} />
        <button type="submit" disabled={!name.trim() || !recipients.trim() || create.isPending}>Schedule</button>
      </form>
      {error && <p role="alert">{error}</p>}
      {schedules && schedules.length > 0 ? (
        <table>
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">When</th>
              <th scope="col">Format</th>
              <th scope="col">Recipients</th>
              <th scope="col">Next run</th>
              <th scope="col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {schedules.map((s) => (
              <tr key={s.id}>
                <td>{s.name}</td>
                <td>{frequencyLabel(s.cronExpression)}</td>
                <td>{s.exportFormat}</td>
                <td>{s.recipients.join(', ')}</td>
                <td>{s.status === 'ACTIVE' && s.nextRunAt ? new Date(s.nextRunAt).toLocaleString() : 'Paused'}</td>
                <td>
                  <button type="button" onClick={() =>
                    setStatus.mutate({ id: s.id, status: s.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE' })}>
                    {s.status === 'ACTIVE' ? 'Pause' : 'Resume'}
                  </button>{' '}
                  <button type="button" onClick={() => remove.mutate(s.id)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p>No scheduled reports yet.</p>
      )}
    </section>
  );
}
