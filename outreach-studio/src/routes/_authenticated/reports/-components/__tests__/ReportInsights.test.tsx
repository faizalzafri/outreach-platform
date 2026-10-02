import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import { ReportInsights } from '../ReportInsights';

describe('ReportInsights', () => {
  it('shows participation, NPS, city activity and the comparison', async () => {
    server.use(
      http.get('/api/reports/dashboard', () => HttpResponse.json({ totalBeneficiaries: 4, activeCities: 3 })),
      http.get('/api/reports/participation-rate', () =>
        HttpResponse.json({ totalRegistered: 10, totalAttended: 8, participationRate: 80, feedbackSubmissionRate: 50 })),
      http.get('/api/reports/sentiment', () => HttpResponse.json({ positive: 0, neutral: 0, negative: 0, total: 0 })),
      http.get('/api/reports/nps', () => HttpResponse.json([{ eventId: 'e1', eventName: 'Coastal Cleanup',
        promoters: 2, passives: 1, detractors: 0, npsScore: 66.7, totalResponses: 3 }])),
      http.get('/api/reports/heatmap', () => HttpResponse.json([{ city: 'Chennai', participantCount: 6, eventCount: 1 }])),
      http.get('/api/reports/comparison', () =>
        HttpResponse.json({ items: [{ label: '2026-09', averageScore: 4.2, feedbackCount: 5, volunteerCount: 7 }] })),
    );

    renderWithProviders(<ReportInsights params={{}} />);

    expect(await screen.findByText('80.0%')).toBeInTheDocument();
    expect(screen.getByText('8 of 10 registered')).toBeInTheDocument();
    expect(screen.getByText('Not analysed yet')).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'Coastal Cleanup' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'Chennai' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '4.20' })).toBeInTheDocument();
  });
});
