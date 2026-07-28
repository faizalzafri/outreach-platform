import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';

// ---------------------------------------------------------------------------
// Mocks
// ---------------------------------------------------------------------------

const mockUseAuth = vi.fn();

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

const mockToastSuccess = vi.fn();
const mockToastError = vi.fn();

vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({
    success: mockToastSuccess,
    error: mockToastError,
    warning: vi.fn(),
    info: vi.fn(),
    toast: vi.fn(),
    dismiss: vi.fn(),
  }),
}));

// Mock Route.useSearch for the reports route
vi.mock('../../index', () => ({
  Route: {
    useSearch: () => ({
      startDate: undefined,
      endDate: undefined,
      granularity: 'DAY',
      tab: 'event',
    }),
  },
}));

// Mock recharts to avoid rendering issues in jsdom
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="responsive-container">{children}</div>
  ),
  LineChart: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="line-chart">{children}</div>
  ),
  BarChart: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="bar-chart">{children}</div>
  ),
  Line: () => <div data-testid="line" />,
  Bar: () => <div data-testid="bar" />,
  XAxis: () => <div data-testid="x-axis" />,
  YAxis: () => <div data-testid="y-axis" />,
  CartesianGrid: () => <div data-testid="cartesian-grid" />,
  Tooltip: () => <div data-testid="tooltip" />,
  Legend: () => <div data-testid="legend" />,
}));

// ---------------------------------------------------------------------------
// Test data
// ---------------------------------------------------------------------------

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

const pocOnlyUser = {
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

const mockReportResponse = {
  timeSeries: [
    { date: '2024-05-01', count: 12, avgScore: 4.1 },
    { date: '2024-05-02', count: 8, avgScore: 4.5 },
    { date: '2024-05-03', count: 15, avgScore: 3.9 },
  ],
  aggregations: [
    {
      dimension: 'Event Alpha',
      submissionCount: 25,
      avgScore: 4.2,
      scoreDistribution: { '1': 1, '2': 3, '3': 5, '4': 10, '5': 6 },
    },
    {
      dimension: 'Event Beta',
      submissionCount: 18,
      avgScore: 3.8,
      scoreDistribution: { '1': 2, '2': 4, '3': 6, '4': 4, '5': 2 },
    },
  ],
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function setupDefaultHandlers() {
  server.use(
    http.get('/api/reports/by-event', () => {
      return HttpResponse.json(mockReportResponse);
    }),
    http.get('/api/reports/by-beneficiary', () => {
      return HttpResponse.json(mockReportResponse);
    }),
    http.get('/api/reports/by-city', () => {
      return HttpResponse.json(mockReportResponse);
    }),
    http.get('/api/reports/by-poc', () => {
      return HttpResponse.json(mockReportResponse);
    }),
  );
}

async function renderReports() {
  const { ReportsContent } = await import('../ReportsContent');
  return renderWithProviders(<ReportsContent />);
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('ReportsContent', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    mockUseAuth.mockReturnValue(adminUser);
    mockToastSuccess.mockClear();
    mockToastError.mockClear();
    setupDefaultHandlers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('Filter change triggers refetch', () => {
    it('refetches data when start date changes', async () => {
      vi.useRealTimers();
      const user = userEvent.setup();
      let requestCount = 0;

      server.use(
        http.get('/api/reports/by-event', () => {
          requestCount++;
          return HttpResponse.json(mockReportResponse);
        }),
      );

      await renderReports();

      await waitFor(() => {
        expect(requestCount).toBeGreaterThanOrEqual(1);
      });

      const initialCount = requestCount;
      const startDateInput = screen.getByLabelText('Start date');
      await user.clear(startDateInput);
      await user.type(startDateInput, '2024-01-01');

      await waitFor(() => {
        expect(requestCount).toBeGreaterThan(initialCount);
      });
    });

    it('refetches data when granularity changes', async () => {
      vi.useRealTimers();
      const user = userEvent.setup();
      let requestCount = 0;

      server.use(
        http.get('/api/reports/by-event', () => {
          requestCount++;
          return HttpResponse.json(mockReportResponse);
        }),
      );

      await renderReports();

      await waitFor(() => {
        expect(requestCount).toBeGreaterThanOrEqual(1);
      });

      const initialCount = requestCount;
      const granularitySelect = screen.getByLabelText('Granularity');
      await user.selectOptions(granularitySelect, 'WEEK');

      await waitFor(() => {
        expect(requestCount).toBeGreaterThan(initialCount);
      });
    });

    it('shows loading skeleton while data is being fetched', async () => {
      vi.useRealTimers();
      server.use(
        http.get('/api/reports/by-event', async () => {
          await new Promise((resolve) => setTimeout(resolve, 200));
          return HttpResponse.json(mockReportResponse);
        }),
      );

      await renderReports();

      expect(screen.getByRole('status', { name: /Loading chart/ })).toBeInTheDocument();

      await waitFor(() => {
        expect(screen.getByText('Feedback Trends')).toBeInTheDocument();
      });
    });
  });

  describe('Aggregation tab switching', () => {
    it('renders all 4 tab buttons', async () => {
      vi.useRealTimers();
      await renderReports();

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: 'By Event' })).toBeInTheDocument();
      });

      expect(screen.getByRole('tab', { name: 'By Beneficiary' })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: 'By City' })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: 'By POC' })).toBeInTheDocument();
    });

    it('marks the active tab as selected', async () => {
      vi.useRealTimers();
      await renderReports();

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: 'By Event' })).toHaveAttribute(
          'aria-selected',
          'true',
        );
      });

      expect(screen.getByRole('tab', { name: 'By City' })).toHaveAttribute(
        'aria-selected',
        'false',
      );
    });

    it('switches active tab and triggers refetch on click', async () => {
      vi.useRealTimers();
      const user = userEvent.setup();
      let lastEndpoint = '';

      server.use(
        http.get('/api/reports/by-event', () => {
          lastEndpoint = 'by-event';
          return HttpResponse.json(mockReportResponse);
        }),
        http.get('/api/reports/by-city', () => {
          lastEndpoint = 'by-city';
          return HttpResponse.json(mockReportResponse);
        }),
      );

      await renderReports();

      await waitFor(() => {
        expect(lastEndpoint).toBe('by-event');
      });

      const cityTab = screen.getByRole('tab', { name: 'By City' });
      await user.click(cityTab);

      await waitFor(() => {
        expect(cityTab).toHaveAttribute('aria-selected', 'true');
      });

      await waitFor(() => {
        expect(lastEndpoint).toBe('by-city');
      });
    });
  });

  describe('Export polling lifecycle', () => {
    it('completes export successfully and shows download link', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });

      server.use(
        http.get('/api/reports/by-event', () => {
          return HttpResponse.json(mockReportResponse);
        }),
        http.post('/api/reports/export', () => {
          return HttpResponse.json({ jobId: 'export-123' }, { status: 202 });
        }),
        http.get('/api/reports/export/:jobId/status', () => {
          return HttpResponse.json({
            jobId: 'export-123',
            status: 'COMPLETED',
            downloadUrl: '/api/reports/export/export-123/download',
          });
        }),
      );

      await renderReports();

      await waitFor(() => {
        expect(screen.getByText('Feedback Trends')).toBeInTheDocument();
      });

      // Click Export button to show format menu
      const exportBtn = screen.getByRole('button', { name: /Export/ });
      await user.click(exportBtn);

      // Select PDF format
      const pdfOption = screen.getByRole('menuitem', { name: 'PDF' });
      await user.click(pdfOption);

      // Advance past the poll interval to trigger the first status check
      await vi.advanceTimersByTimeAsync(6_000);

      await waitFor(() => {
        expect(screen.getByText('Download Report')).toBeInTheDocument();
      });

      expect(mockToastSuccess).toHaveBeenCalledWith('Export completed successfully.');
    });

    it('shows error state when export fails', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });

      server.use(
        http.get('/api/reports/by-event', () => {
          return HttpResponse.json(mockReportResponse);
        }),
        http.post('/api/reports/export', () => {
          return HttpResponse.json({ jobId: 'export-fail' }, { status: 202 });
        }),
        http.get('/api/reports/export/:jobId/status', () => {
          return HttpResponse.json({
            jobId: 'export-fail',
            status: 'FAILED',
          });
        }),
      );

      await renderReports();

      await waitFor(() => {
        expect(screen.getByText('Feedback Trends')).toBeInTheDocument();
      });

      const exportBtn = screen.getByRole('button', { name: /Export/ });
      await user.click(exportBtn);

      const csvOption = screen.getByRole('menuitem', { name: 'CSV' });
      await user.click(csvOption);

      // Advance past the poll interval to trigger status check
      await vi.advanceTimersByTimeAsync(6_000);

      await waitFor(() => {
        expect(screen.getByText(/Export failed/)).toBeInTheDocument();
      });

      expect(mockToastError).toHaveBeenCalledWith('Export failed. Please try again.');
    });

    it('shows timeout state when export exceeds max duration', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });

      server.use(
        http.get('/api/reports/by-event', () => {
          return HttpResponse.json(mockReportResponse);
        }),
        http.post('/api/reports/export', () => {
          return HttpResponse.json({ jobId: 'export-timeout' }, { status: 202 });
        }),
        http.get('/api/reports/export/:jobId/status', () => {
          // Always return IN_PROGRESS to trigger timeout
          return HttpResponse.json({
            jobId: 'export-timeout',
            status: 'IN_PROGRESS',
          });
        }),
      );

      await renderReports();

      await waitFor(() => {
        expect(screen.getByText('Feedback Trends')).toBeInTheDocument();
      });

      const exportBtn = screen.getByRole('button', { name: /Export/ });
      await user.click(exportBtn);

      const excelOption = screen.getByRole('menuitem', { name: 'Excel' });
      await user.click(excelOption);

      // Advance time past the 2-minute max export duration
      await vi.advanceTimersByTimeAsync(130_000);

      await waitFor(() => {
        expect(screen.getByText(/Export timed out/)).toBeInTheDocument();
      });

      expect(mockToastError).toHaveBeenCalledWith('Export timed out. Please try again.');
    });

    it('shows error when export POST request fails', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });

      server.use(
        http.get('/api/reports/by-event', () => {
          return HttpResponse.json(mockReportResponse);
        }),
        http.post('/api/reports/export', () => {
          return HttpResponse.json(
            { message: 'Server error' },
            { status: 500 },
          );
        }),
      );

      await renderReports();

      await waitFor(() => {
        expect(screen.getByText('Feedback Trends')).toBeInTheDocument();
      });

      const exportBtn = screen.getByRole('button', { name: /Export/ });
      await user.click(exportBtn);

      const pdfOption = screen.getByRole('menuitem', { name: 'PDF' });
      await user.click(pdfOption);

      await waitFor(() => {
        expect(mockToastError).toHaveBeenCalledWith('Failed to start export.');
      });
    });
  });

  describe('ROLE_POC filter restriction', () => {
    it('disables POC filter for POC-only users', async () => {
      vi.useRealTimers();
      mockUseAuth.mockReturnValue(pocOnlyUser);

      await renderReports();

      await waitFor(() => {
        expect(screen.getByLabelText('Filter by POCs')).toBeDisabled();
      });
    });

    it('enables POC filter for admin users', async () => {
      vi.useRealTimers();
      mockUseAuth.mockReturnValue(adminUser);

      await renderReports();

      await waitFor(() => {
        expect(screen.getByLabelText('Filter by POCs')).not.toBeDisabled();
      });
    });

    it('POC-only user has POC filter with aria-disabled attribute', async () => {
      vi.useRealTimers();
      mockUseAuth.mockReturnValue(pocOnlyUser);

      await renderReports();

      await waitFor(() => {
        const pocFilter = screen.getByLabelText('Filter by POCs');
        expect(pocFilter).toHaveAttribute('aria-disabled', 'true');
      });
    });
  });
});
