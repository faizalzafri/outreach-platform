import { describe, it, expect } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import { ReportSchedules } from '../ReportSchedules';

describe('ReportSchedules', () => {
  it('schedules a report for the given recipients and lists it', async () => {
    let created: unknown;
    const schedules: unknown[] = [];
    server.use(
      http.get('/api/reports/scheduled', () => HttpResponse.json(schedules)),
      http.post('/api/reports/scheduled', async ({ request }) => {
        created = await request.json();
        schedules.push({ id: 's-1', name: 'Weekly scores', cronExpression: '0 0 8 * * MON', exportFormat: 'CSV',
          recipients: ['pmo@example.com', 'lead@example.com'], status: 'ACTIVE', nextRunAt: '2026-10-05T02:30:00Z' });
        return HttpResponse.json(schedules[0], { status: 201 });
      }),
    );

    renderWithProviders(<ReportSchedules />);
    await userEvent.type(screen.getByLabelText('Schedule name'), 'Weekly scores');
    await userEvent.selectOptions(screen.getByLabelText('Format'), 'CSV');
    await userEvent.type(screen.getByLabelText('Recipients'), 'pmo@example.com, lead@example.com');
    await userEvent.click(screen.getByRole('button', { name: 'Schedule' }));

    await waitFor(() => expect(created).toEqual({
      name: 'Weekly scores', reportType: 'BY_EVENT', cronExpression: '0 0 8 * * MON', exportFormat: 'CSV',
      recipients: ['pmo@example.com', 'lead@example.com'],
    }));
    expect(await screen.findByRole('cell', { name: 'Every Monday' })).toBeInTheDocument();
  });
});
