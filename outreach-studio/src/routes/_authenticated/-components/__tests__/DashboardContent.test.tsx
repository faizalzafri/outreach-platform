import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';

// Mock useAuth to control the user's roles in tests
const mockUseAuth = vi.fn();

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// ---------------------------------------------------------------------------
// Mock data
// ---------------------------------------------------------------------------

const mockKpis = {
  totalEvents: 42,
  activeEvents: 8,
  totalVolunteers: 356,
  averageFeedbackScore: 4.3,
  pendingFeedback: 12,
  notificationDeliveryRate: 97.5,
};

const mockTrends = {
  feedbackTrends: [
    { date: '2024-05-01', count: 12, avgScore: 4.1 },
    { date: '2024-05-02', count: 8, avgScore: 4.5 },
  ],
  eventStatusDistribution: [
    { status: 'ACTIVE', count: 10 },
    { status: 'COMPLETED', count: 5 },
  ],
  feedbackScoreDistribution: [
    { score: 1, count: 3 },
    { score: 2, count: 8 },
    { score: 3, count: 22 },
    { score: 4, count: 45 },
    { score: 5, count: 32 },
  ],
};

const mockLifecycleStats = [
  { status: 'DRAFT', count: 5 },
  { status: 'ACTIVE', count: 8 },
  { status: 'COMPLETED', count: 12 },
];

const adminUser = {
  user: {
    sub: 'admin-001',
    name: 'Admin User',
    email: 'admin@outreach.dev',
    roles: ['ROLE_ADMIN', 'ROLE_PMO'],
  },
  isAuthenticated: true,
  isLoading: false,
  login: vi.fn(),
  logout: vi.fn(),
};

const pocUser = {
  user: {
    sub: 'poc-001',
    name: 'POC User',
    email: 'poc@outreach.dev',
    roles: ['ROLE_POC'],
  },
  isAuthenticated: true,
  isLoading: false,
  login: vi.fn(),
  logout: vi.fn(),
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function setupDefaultHandlers() {
  server.use(
    http.get('/api/reports/dashboard/kpis', () => {
      return HttpResponse.json(mockKpis);
    }),
    http.get('/api/reports/dashboard/trends', () => {
      return HttpResponse.json(mockTrends);
    }),
    http.get('/api/events/lifecycle-stats', () => {
      return HttpResponse.json(mockLifecycleStats);
    }),
  );
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('DashboardContent', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue(adminUser);
    setupDefaultHandlers();
  });

  // Lazy import so the module picks up the mock
  async function renderDashboard() {
    const { DashboardContent } = await import('../DashboardContent');
    return renderWithProviders(<DashboardContent />);
  }

  describe('KPI card rendering with mocked data', () => {
    it('renders all 6 KPI cards with correct values', async () => {
      await renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Total Events')).toBeInTheDocument();
      });

      expect(screen.getByText('42')).toBeInTheDocument();
      expect(screen.getByText('Active Events')).toBeInTheDocument();
      expect(screen.getByText('8')).toBeInTheDocument();
      expect(screen.getByText('Total Volunteers')).toBeInTheDocument();
      expect(screen.getByText('356')).toBeInTheDocument();
      expect(screen.getByText('Avg Feedback Score')).toBeInTheDocument();
      expect(screen.getByText('4.3')).toBeInTheDocument();
      expect(screen.getByText('Pending Feedback')).toBeInTheDocument();
      expect(screen.getByText('12')).toBeInTheDocument();
      expect(screen.getByText('Notification Delivery Rate')).toBeInTheDocument();
      expect(screen.getByText('97.5%')).toBeInTheDocument();
    });

    it('shows loading skeletons while KPIs are fetching', async () => {
      server.use(
        http.get('/api/reports/dashboard/kpis', async () => {
          await new Promise((resolve) => setTimeout(resolve, 500));
          return HttpResponse.json(mockKpis);
        }),
      );

      await renderDashboard();

      expect(screen.getByRole('status', { name: /Loading KPIs/ })).toBeInTheDocument();

      await waitFor(() => {
        expect(screen.getByText('42')).toBeInTheDocument();
      });
    });

    it('renders the Dashboard heading', async () => {
      await renderDashboard();

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Dashboard', level: 1 })).toBeInTheDocument();
      });
    });
  });

  describe('Date range filter triggers refetch', () => {
    it('refetches trends when start date changes', async () => {
      const user = userEvent.setup();
      let requestCount = 0;
      let lastStartDate: string | null = null;

      server.use(
        http.get('/api/reports/dashboard/trends', ({ request }) => {
          requestCount++;
          const url = new URL(request.url);
          lastStartDate = url.searchParams.get('startDate');
          return HttpResponse.json(mockTrends);
        }),
      );

      await renderDashboard();

      await waitFor(() => {
        expect(requestCount).toBeGreaterThanOrEqual(1);
      });

      const startDateInput = screen.getByLabelText('Start date');
      await user.clear(startDateInput);
      await user.type(startDateInput, '2024-01-01');

      await waitFor(() => {
        expect(lastStartDate).toBe('2024-01-01');
      });
    });

    it('refetches trends when end date changes', async () => {
      const user = userEvent.setup();
      let lastEndDate: string | null = null;

      server.use(
        http.get('/api/reports/dashboard/trends', ({ request }) => {
          const url = new URL(request.url);
          lastEndDate = url.searchParams.get('endDate');
          return HttpResponse.json(mockTrends);
        }),
      );

      await renderDashboard();

      await waitFor(() => {
        expect(lastEndDate).not.toBeNull();
      });

      const endDateInput = screen.getByLabelText('End date');
      await user.clear(endDateInput);
      await user.type(endDateInput, '2024-12-31');

      await waitFor(() => {
        expect(lastEndDate).toBe('2024-12-31');
      });
    });

    it('refetches trends when granularity changes', async () => {
      const user = userEvent.setup();
      let lastGranularity: string | null = null;

      server.use(
        http.get('/api/reports/dashboard/trends', ({ request }) => {
          const url = new URL(request.url);
          lastGranularity = url.searchParams.get('granularity');
          return HttpResponse.json(mockTrends);
        }),
      );

      await renderDashboard();

      await waitFor(() => {
        expect(lastGranularity).toBe('day');
      });

      const granularitySelect = screen.getByLabelText('Granularity');
      await user.selectOptions(granularitySelect, 'WEEK');

      await waitFor(() => {
        expect(lastGranularity).toBe('week');
      });
    });
  });

  describe('Per-widget error isolation', () => {
    it('shows KPI error without affecting chart widgets', async () => {
      server.use(
        http.get('/api/reports/dashboard/kpis', () => {
          return HttpResponse.json(
            { message: 'KPI service unavailable' },
            { status: 500 },
          );
        }),
      );

      await renderDashboard();

      // KPI section should show error with retry button
      await waitFor(() => {
        expect(screen.getByText(/Retry/)).toBeInTheDocument();
      });

      // Chart titles should still render (charts have their own queries)
      expect(screen.getByText('Feedback Trends')).toBeInTheDocument();
      expect(screen.getByText('Event Status Distribution')).toBeInTheDocument();
      expect(screen.getByText('Feedback Score Distribution')).toBeInTheDocument();
    });

    it('shows trends error without affecting KPI cards', async () => {
      server.use(
        http.get('/api/reports/dashboard/trends', () => {
          return HttpResponse.json(
            { message: 'Trends service unavailable' },
            { status: 500 },
          );
        }),
        http.get('/api/events/lifecycle-stats', () => {
          return HttpResponse.json(
            { message: 'Lifecycle stats unavailable' },
            { status: 500 },
          );
        }),
      );

      await renderDashboard();

      // KPI cards should still render
      await waitFor(() => {
        expect(screen.getByText('Total Events')).toBeInTheDocument();
        expect(screen.getByText('42')).toBeInTheDocument();
      });
    });

    it('shows retry button in KPI error state and refetches on click', async () => {
      let callCount = 0;

      server.use(
        http.get('/api/reports/dashboard/kpis', () => {
          callCount++;
          if (callCount === 1) {
            return HttpResponse.json(
              { message: 'Service unavailable' },
              { status: 500 },
            );
          }
          return HttpResponse.json(mockKpis);
        }),
      );

      const user = userEvent.setup();
      await renderDashboard();

      await waitFor(() => {
        expect(screen.getByText(/Retry/)).toBeInTheDocument();
      });

      await user.click(screen.getByText(/Retry/));

      await waitFor(() => {
        expect(screen.getByText('42')).toBeInTheDocument();
      });
    });
  });

  describe('Role restriction (ROLE_POC sees no dashboard)', () => {
    it('shows Access Denied for ROLE_POC user', async () => {
      mockUseAuth.mockReturnValue(pocUser);

      await renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Access Denied')).toBeInTheDocument();
      });

      // KPI data should NOT be rendered
      expect(screen.queryByText('Total Events')).not.toBeInTheDocument();
    });

    it('renders dashboard content for ROLE_ADMIN user', async () => {
      mockUseAuth.mockReturnValue(adminUser);

      await renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Dashboard')).toBeInTheDocument();
        expect(screen.getByText('Total Events')).toBeInTheDocument();
      });
    });
  });
});
