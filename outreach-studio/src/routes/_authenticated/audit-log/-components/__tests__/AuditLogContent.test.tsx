import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';

// Mock useAuth to provide authenticated user context
const mockUseAuth = vi.fn();

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// Mock useToast to capture error calls
const mockToastError = vi.fn();
vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({
    toast: vi.fn(),
    success: vi.fn(),
    error: mockToastError,
    warning: vi.fn(),
    info: vi.fn(),
    dismiss: vi.fn(),
  }),
}));

// Mock Route.useSearch
vi.mock('../../index', () => ({
  Route: {
    useSearch: () => ({
      page: 1,
      size: 50,
      user: undefined,
      action: undefined,
      resourceType: undefined,
      startDate: undefined,
      endDate: undefined,
    }),
  },
}));

// ---------------------------------------------------------------------------
// Mock data
// ---------------------------------------------------------------------------

const mockAuditEntries = {
  content: [
    {
      id: 'audit-001',
      timestamp: '2024-06-02T15:30:00Z',
      user: 'priya_sharma',
      action: 'EVENT_CREATED',
      resourceType: 'EVENT',
      resourceId: 'evt-001',
      ipAddress: '192.168.1.100',
      payload: {
        eventName: 'Annual Volunteer Drive',
        eventCode: 'EVT2024001',
      },
    },
    {
      id: 'audit-002',
      timestamp: '2024-06-02T14:00:00Z',
      user: 'admin_user',
      action: 'USER_ROLE_CHANGED',
      resourceType: 'USER',
      resourceId: 'user-002',
      ipAddress: '10.0.0.50',
      payload: {
        previousRole: 'ROLE_POC',
        newRole: 'ROLE_PMO',
      },
    },
    {
      id: 'audit-003',
      timestamp: '2024-06-02T12:15:00Z',
      user: 'rahul_verma',
      action: 'VOLUNTEER_ENROLLED',
      resourceType: 'VOLUNTEER',
      resourceId: 'EMP005',
      ipAddress: '172.16.0.25',
      payload: {
        eventId: 'evt-001',
        employeeId: 'EMP005',
      },
    },
  ],
  nextCursor: 'cursor-abc123',
  hasMore: true,
  totalElements: 150,
};

const adminUser = {
  user: {
    sub: 'admin-001',
    name: 'Admin User',
    email: 'admin@outreach.dev',
    roles: ['ROLE_ADMIN'],
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
    http.get('/api/admin/audit-log', () => {
      return HttpResponse.json(mockAuditEntries);
    }),
  );
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('AuditLogContent', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    mockUseAuth.mockReturnValue(adminUser);
    mockToastError.mockClear();
    setupDefaultHandlers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  async function renderAuditLog() {
    const { AuditLogContent } = await import('../AuditLogContent');
    return renderWithProviders(<AuditLogContent />);
  }

  // =========================================================================
  // Audit Log Row Expansion and JSON Display
  // =========================================================================

  describe('Row expansion and JSON display', () => {
    it('expands a row to show JSON payload when clicked', async () => {
      vi.useRealTimers();
      const user = userEvent.setup();
      await renderAuditLog();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      // Click on the first row to expand
      const row = screen.getByText('priya_sharma').closest('tr')!;
      await user.click(row);

      // Should show the JSON payload
      await waitFor(() => {
        expect(screen.getByText(/"eventName": "Annual Volunteer Drive"/)).toBeInTheDocument();
      });
    });

    it('collapses an expanded row when clicked again', async () => {
      vi.useRealTimers();
      const user = userEvent.setup();
      await renderAuditLog();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      const row = screen.getByText('priya_sharma').closest('tr')!;

      // Expand
      await user.click(row);
      await waitFor(() => {
        expect(screen.getByText(/"eventName": "Annual Volunteer Drive"/)).toBeInTheDocument();
      });

      // Collapse
      await user.click(row);
      await waitFor(() => {
        expect(screen.queryByText(/"eventName": "Annual Volunteer Drive"/)).not.toBeInTheDocument();
      });
    });

    it('displays formatted JSON with indentation in the expanded row', async () => {
      vi.useRealTimers();
      const user = userEvent.setup();
      await renderAuditLog();

      await waitFor(() => {
        expect(screen.getByText('admin_user')).toBeInTheDocument();
      });

      // Expand the second row
      const row = screen.getByText('admin_user').closest('tr')!;
      await user.click(row);

      await waitFor(() => {
        // JSON.stringify with indent 2 renders keys with quotes
        expect(screen.getByText(/"previousRole": "ROLE_POC"/)).toBeInTheDocument();
        expect(screen.getByText(/"newRole": "ROLE_PMO"/)).toBeInTheDocument();
      });
    });

    it('shows aria-expanded attribute on expandable rows', async () => {
      vi.useRealTimers();
      const user = userEvent.setup();
      await renderAuditLog();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      const row = screen.getByText('priya_sharma').closest('tr')!;
      expect(row).toHaveAttribute('aria-expanded', 'false');

      await user.click(row);

      expect(row).toHaveAttribute('aria-expanded', 'true');
    });
  });

  // =========================================================================
  // Audit Log Filter Debounce
  // =========================================================================

  describe('Filter debounce', () => {
    it('debounces user text filter by 300ms before triggering API call', async () => {
      let requestCount = 0;
      server.use(
        http.get('/api/admin/audit-log', ({ request }) => {
          requestCount++;
          const url = new URL(request.url);
          const userParam = url.searchParams.get('user');
          const content = userParam
            ? mockAuditEntries.content.filter((e) =>
                e.user.toLowerCase().includes(userParam.toLowerCase()),
              )
            : mockAuditEntries.content;
          return HttpResponse.json({
            ...mockAuditEntries,
            content,
          });
        }),
      );

      await renderAuditLog();

      // Wait for initial data load
      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      // Record the request count after initial load
      const initialCount = requestCount;

      // Type in the user filter input rapidly
      const userInput = screen.getByLabelText('User');
      await act(async () => {
        // Simulate typing character by character
        await userEvent.setup({ advanceTimers: vi.advanceTimersByTime }).type(userInput, 'priya');
      });

      // Immediately after typing, should NOT have made a new request yet (debounce)
      expect(requestCount).toBe(initialCount);

      // Advance timers past the 300ms debounce period
      await act(async () => {
        vi.advanceTimersByTime(350);
      });

      // Now the debounced value should have triggered a new fetch
      await waitFor(() => {
        expect(requestCount).toBeGreaterThan(initialCount);
      });
    });

    it('does not debounce select filter changes', async () => {
      let lastActionParam: string | null = null;
      server.use(
        http.get('/api/admin/audit-log', ({ request }) => {
          const url = new URL(request.url);
          lastActionParam = url.searchParams.get('action');
          return HttpResponse.json(mockAuditEntries);
        }),
      );

      await renderAuditLog();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      // Change the action select filter
      const actionSelect = screen.getByLabelText('Action');
      await act(async () => {
        await userEvent.setup({ advanceTimers: vi.advanceTimersByTime }).selectOptions(actionSelect, 'CREATE');
      });

      // Select filters should trigger immediately (no debounce on selects)
      await act(async () => {
        vi.advanceTimersByTime(50);
      });

      await waitFor(() => {
        expect(lastActionParam).toBe('CREATE');
      });
    });
  });
});
