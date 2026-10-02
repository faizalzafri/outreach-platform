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
    volunteerId: 'c0000000-0000-0000-0000-000000000001',
    volunteerName: 'Rajesh Kumar',
    anonymous: false,
    score: 4,
    category: 'Organization',
    sentiment: 'POSITIVE',
    status: 'SUBMITTED',
    submittedAt: '2024-06-02T10:00:00Z',
  },
  {
    id: 'fb-002',
    eventName: 'Annual Volunteer Drive',
    volunteerId: 'c0000000-0000-0000-0000-000000000002',
    volunteerName: null,
    anonymous: true,
    score: 2,
    category: 'Overall',
    sentiment: 'NEGATIVE',
    status: 'SUBMITTED',
    submittedAt: '2024-06-03T10:00:00Z',
  },
];

async function renderFeedbackList() {
  const { FeedbackListContent } = await import('../FeedbackListContent');
  return renderWithProviders(<FeedbackListContent />);
}

describe('FeedbackListContent', () => {
  it('displays feedback rows from the API response', async () => {
    server.use(
      http.get('/api/feedback/search', () => {
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
      expect(screen.getAllByText('Annual Volunteer Drive')).toHaveLength(2);
    });
    expect(screen.getByText('Rajesh Kumar')).toBeInTheDocument();
    expect(screen.getByText('Anonymous')).toBeInTheDocument();
    expect(screen.queryByText('c0000000-0000-0000-0000-000000000001')).not.toBeInTheDocument();
    expect(screen.getByText('POSITIVE')).toBeInTheDocument();
  });

  it('shows the empty message when no feedback is returned', async () => {
    server.use(
      http.get('/api/feedback/search', () => {
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
        http.get('/api/feedback/search', () => {
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
        expect(screen.getAllByText('Annual Volunteer Drive')).not.toHaveLength(0);
      });

      expect(await axe(container)).toHaveNoViolations();
    });
  });
});
