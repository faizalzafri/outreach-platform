import { describe, it, expect } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { axe } from 'jest-axe';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';

const MOCK_FEEDBACK = [
  {
    id: 'fb-001',
    eventName: 'Annual Volunteer Drive',
    volunteerId: 'EMP001',
    score: 4,
    category: 'Organization',
    sentiment: 'POSITIVE',
    status: 'SUBMITTED',
    submittedAt: '2024-06-02T10:00:00Z',
  },
];

async function renderFeedbackList() {
  const { FeedbackListContent } = await import('../FeedbackListContent');
  return renderWithProviders(<FeedbackListContent />);
}

describe('FeedbackListContent', () => {
  it('displays feedback rows from the API response', async () => {
    server.use(
      http.get('/api/feedback', () => {
        return HttpResponse.json({
          content: MOCK_FEEDBACK,
          totalElements: MOCK_FEEDBACK.length,
          totalPages: 1,
          page: 0,
          size: 10,
        });
      }),
    );

    await renderFeedbackList();

    await waitFor(() => {
      expect(screen.getByText('Annual Volunteer Drive')).toBeInTheDocument();
    });
    expect(screen.getByText('POSITIVE')).toBeInTheDocument();
    expect(screen.getByText('SUBMITTED')).toBeInTheDocument();
  });

  it('shows the empty message when no feedback is returned', async () => {
    server.use(
      http.get('/api/feedback', () => {
        return HttpResponse.json({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 10 });
      }),
    );

    await renderFeedbackList();

    await waitFor(() => {
      expect(screen.getByText('No feedback records found.')).toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('has no axe violations once loaded', async () => {
      server.use(
        http.get('/api/feedback', () => {
          return HttpResponse.json({
            content: MOCK_FEEDBACK,
            totalElements: MOCK_FEEDBACK.length,
            totalPages: 1,
            page: 0,
            size: 10,
          });
        }),
      );

      const { container } = await renderFeedbackList();

      await waitFor(() => {
        expect(screen.getByText('Annual Volunteer Drive')).toBeInTheDocument();
      });

      expect(await axe(container)).toHaveNoViolations();
    });
  });
});
