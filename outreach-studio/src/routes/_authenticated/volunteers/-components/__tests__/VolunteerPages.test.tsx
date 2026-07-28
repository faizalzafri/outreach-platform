import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import { buildVolunteer } from '@/test/factories/volunteer.factory';
import type { Volunteer } from '@/types/domain';
import type { PageResponse } from '@/types/api';

// ---------------------------------------------------------------------------
// Mock TanStack Router
// ---------------------------------------------------------------------------

const mockNavigate = vi.fn();
const mockSearchParams = vi.fn();
const mockRouteParams = vi.fn();

vi.mock('@tanstack/react-router', () => ({
  createFileRoute: () => (opts: unknown) => opts,
  Link: ({ children, to, ...props }: { children: React.ReactNode; to: string; [k: string]: unknown }) => (
    <a href={to} data-testid={`link-${to}`} {...props}>{children}</a>
  ),
  useNavigate: () => mockNavigate,
}));

// Mock the route modules that components import for useSearch/useParams
vi.mock('../../index', () => ({
  Route: {
    useSearch: () => mockSearchParams(),
  },
}));

vi.mock('../../$employeeId', () => ({
  Route: {
    useParams: () => mockRouteParams(),
  },
}));

// Mock event route modules used by EventDetailContent
// EventDetailContent.tsx imports Route from '../$eventId' which resolves to
// src/routes/_authenticated/events/$eventId.tsx
vi.mock('@/routes/_authenticated/events/$eventId', () => ({
  Route: {
    useSearch: () => mockSearchParams(),
    useParams: () => mockRouteParams(),
    fullPath: '/_authenticated/events/$eventId',
  },
}));

// Mock useAuth to provide authentication context
const mockUseAuth = vi.fn();
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// Mock recharts to avoid rendering issues in test
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div data-testid="responsive-container">{children}</div>,
  LineChart: ({ children }: { children: React.ReactNode }) => <div data-testid="line-chart">{children}</div>,
  Line: () => <div data-testid="line" />,
  XAxis: () => <div data-testid="x-axis" />,
  YAxis: () => <div data-testid="y-axis" />,
  CartesianGrid: () => <div data-testid="cartesian-grid" />,
  Tooltip: () => <div data-testid="tooltip" />,
}));

// ---------------------------------------------------------------------------
// Test data
// ---------------------------------------------------------------------------

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

function createUserPage(users: Array<Record<string, unknown>>, page = 0, size = 10) {
  return {
    content: users,
    totalElements: users.length,
    totalPages: Math.ceil(users.length / size),
    page,
    size,
  };
}

function _createVolunteerPage(volunteers: Volunteer[], page = 0, size = 10): PageResponse<Volunteer> {
  return {
    content: volunteers,
    totalElements: volunteers.length,
    totalPages: Math.ceil(volunteers.length / size),
    page,
    size,
  };
}

// ---------------------------------------------------------------------------
// Volunteer List Tests — Search Debounce
// ---------------------------------------------------------------------------

describe('VolunteerListContent', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue(adminUser);
    mockSearchParams.mockReturnValue({ page: 1, size: 10, search: undefined });
  });

  async function renderVolunteerList() {
    const { VolunteerListContent } = await import('../VolunteerListContent');
    return renderWithProviders(<VolunteerListContent />);
  }

  describe('search debounce', () => {
    it('does not trigger a refetch immediately on typing', async () => {
      const user = userEvent.setup();
      let requestCount = 0;

      server.use(
        http.get('/api/admin/users', () => {
          requestCount++;
          return HttpResponse.json(createUserPage([
            { id: 'u1', username: 'priya_sharma', email: 'priya@co.com', role: 'ROLE_POC', enabled: true },
          ]));
        }),
      );

      await renderVolunteerList();

      // Wait for initial load
      await waitFor(() => {
        expect(requestCount).toBeGreaterThanOrEqual(1);
      });

      const initialCount = requestCount;
      const searchInput = screen.getByLabelText('Search users');
      await user.type(searchInput, 'priya');

      // Immediately after typing, no additional request should have fired
      // (debounce hasn't elapsed yet)
      expect(requestCount).toBe(initialCount);
    });

    it('triggers a refetch after the debounce delay when search changes', async () => {
      const user = userEvent.setup();
      let requestCount = 0;

      server.use(
        http.get('/api/admin/users', () => {
          requestCount++;
          return HttpResponse.json(createUserPage([
            { id: 'u1', username: 'priya_sharma', email: 'priya@co.com', role: 'ROLE_POC', enabled: true },
          ]));
        }),
      );

      await renderVolunteerList();

      // Wait for initial load
      await waitFor(() => {
        expect(requestCount).toBeGreaterThanOrEqual(1);
      });

      const countAfterLoad = requestCount;
      const searchInput = screen.getByLabelText('Search users');
      await user.type(searchInput, 'anita');

      // After debounce delay (300ms), a new request should be triggered
      await waitFor(() => {
        expect(requestCount).toBeGreaterThan(countAfterLoad);
      }, { timeout: 1000 });
    });

    it('only triggers one refetch when user types multiple characters quickly', async () => {
      const user = userEvent.setup();
      let requestCount = 0;

      server.use(
        http.get('/api/admin/users', () => {
          requestCount++;
          return HttpResponse.json(createUserPage([
            { id: 'u1', username: 'vikram_singh', email: 'vikram@co.com', role: 'ROLE_POC', enabled: true },
          ]));
        }),
      );

      await renderVolunteerList();

      // Wait for initial load to complete
      await waitFor(() => {
        expect(requestCount).toBeGreaterThanOrEqual(1);
      });

      const countAfterLoad = requestCount;
      const searchInput = screen.getByLabelText('Search users');

      // Type several characters quickly (within debounce window)
      await user.type(searchInput, 'vikram');

      // Wait for the debounce to fire (should only trigger ONE additional request)
      await waitFor(() => {
        expect(requestCount).toBeGreaterThan(countAfterLoad);
      }, { timeout: 1000 });

      // Record count immediately after first debounce fires
      const countAfterDebounce = requestCount;

      // Wait a bit more to ensure no extra requests are fired
      await new Promise((resolve) => setTimeout(resolve, 400));
      expect(requestCount).toBe(countAfterDebounce);
    });
  });
});

// ---------------------------------------------------------------------------
// Volunteer Detail Tests — Availability Optimistic Update and Rollback
// ---------------------------------------------------------------------------

describe('VolunteerDetailContent', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue(adminUser);
    mockRouteParams.mockReturnValue({ employeeId: 'EMP001' });
  });

  function setupVolunteerDetailHandlers(volunteer?: Partial<Volunteer>) {
    const defaultVolunteer = buildVolunteer({
      employeeId: 'EMP001',
      name: 'Anita Desai',
      email: 'anita@company.com',
      department: 'Engineering',
      location: 'Mumbai',
      skills: ['JavaScript', 'React'],
      availability: 'AVAILABLE',
      totalEvents: 12,
      ...volunteer,
    });

    server.use(
      http.get('/api/events/volunteers/EMP001', () => {
        return HttpResponse.json(defaultVolunteer);
      }),
      http.get('/api/events/volunteers/EMP001/history', () => {
        return HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          page: 0,
          size: 5,
        });
      }),
      http.get('/api/events/volunteers/EMP001/score-trend', () => {
        return HttpResponse.json([]);
      }),
    );

    return defaultVolunteer;
  }

  async function renderVolunteerDetail() {
    const { VolunteerDetailContent } = await import('../VolunteerDetailContent');
    return renderWithProviders(<VolunteerDetailContent />);
  }

  describe('availability optimistic update', () => {
    it('updates availability badge immediately on selection change', async () => {
      const user = userEvent.setup();
      setupVolunteerDetailHandlers({ availability: 'AVAILABLE' });

      let mutationCalled = false;
      server.use(
        http.put('/api/events/volunteers/EMP001/availability', async ({ request }) => {
          mutationCalled = true;
          // Simulate a slightly delayed response
          await new Promise((resolve) => setTimeout(resolve, 200));
          const body = (await request.json()) as Record<string, unknown>;
          return HttpResponse.json({
            employeeId: 'EMP001',
            name: 'Anita Desai',
            email: 'anita@company.com',
            department: 'Engineering',
            location: 'Mumbai',
            skills: ['JavaScript', 'React'],
            availability: body.availability,
            totalEvents: 12,
            averageScore: 4.5,
            joinDate: '2022-03-15T00:00:00Z',
          });
        }),
      );

      await renderVolunteerDetail();

      // Wait for volunteer data to load
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Anita Desai' })).toBeInTheDocument();
      });

      // Verify initial availability via select value
      const select = screen.getByLabelText('Update availability');
      expect(select).toHaveValue('AVAILABLE');

      // Change availability
      await user.selectOptions(select, 'UNAVAILABLE');

      // The mutation should be called and select should reflect the new value
      await waitFor(() => {
        expect(mutationCalled).toBe(true);
      });

      // After mutation completes, the select should show the updated value
      await waitFor(() => {
        expect(select).toHaveValue('UNAVAILABLE');
      });
    });

    it('rolls back availability on server rejection', async () => {
      const user = userEvent.setup();
      setupVolunteerDetailHandlers({ availability: 'AVAILABLE' });

      // Override with error response
      server.use(
        http.put('/api/events/volunteers/EMP001/availability', () => {
          return HttpResponse.json(
            { error: 'Bad Request', message: 'Invalid availability value' },
            { status: 400, headers: { 'X-Correlation-ID': 'test-corr-id' } },
          );
        }),
      );

      await renderVolunteerDetail();

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Anita Desai' })).toBeInTheDocument();
      });

      // Verify initial availability via select value
      const select = screen.getByLabelText('Update availability');
      expect(select).toHaveValue('AVAILABLE');

      // Attempt to change availability
      await user.selectOptions(select, 'ON_LEAVE');

      // After server rejection, should show error and rollback
      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });

      // The select should be rolled back to original value
      await waitFor(() => {
        expect(select).toHaveValue('AVAILABLE');
      });
    });

    it('displays error message on availability update failure', async () => {
      const user = userEvent.setup();
      setupVolunteerDetailHandlers({ availability: 'AVAILABLE' });

      server.use(
        http.put('/api/events/volunteers/EMP001/availability', () => {
          return HttpResponse.json(
            { error: 'Bad Request', message: 'Invalid availability value' },
            { status: 400, headers: { 'X-Correlation-ID': 'test-corr-id' } },
          );
        }),
      );

      await renderVolunteerDetail();

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Anita Desai' })).toBeInTheDocument();
      });

      const select = screen.getByLabelText('Update availability');
      await user.selectOptions(select, 'UNAVAILABLE');

      // Error message should appear
      await waitFor(() => {
        const alert = screen.getByRole('alert');
        expect(alert).toBeInTheDocument();
        expect(alert.textContent).toBeTruthy();
      });
    });
  });
});

// ---------------------------------------------------------------------------
// Enrollment Error Handling Tests (VolunteersTab in EventDetailContent)
// ---------------------------------------------------------------------------

describe('Volunteer Enrollment Error Handling', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue(adminUser);
    mockRouteParams.mockReturnValue({ eventId: 'evt-001' });
    mockSearchParams.mockReturnValue({ tab: 'volunteers' });

    // Setup default event detail handler
    server.use(
      http.get('/api/events/evt-001', () => {
        return HttpResponse.json({
          id: 'evt-001',
          eventCode: 'EVT2024001',
          eventName: 'Annual Volunteer Drive',
          description: 'A large-scale initiative.',
          status: 'PUBLISHED',
          eventDate: '2024-06-01',
          eventEndDate: '2024-06-03',
          city: 'Mumbai',
          venue: 'Convention Center',
          category: 'CSR',
          maxVolunteers: 200,
          registeredCount: 45,
          attendedCount: 38,
          createdAt: '2024-05-01T10:00:00Z',
          updatedAt: '2024-05-15T14:30:00Z',
          createdBy: 'admin',
        });
      }),
      http.get('/api/events/evt-001/volunteers', () => {
        return HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          page: 0,
          size: 50,
        });
      }),
    );
  });

  async function renderEventDetail() {
    const { EventDetailContent } = await import(
      '@/routes/_authenticated/events/-components/EventDetailContent'
    );
    return renderWithProviders(<EventDetailContent />);
  }

  it('shows duplicate enrollment error when volunteer is already enrolled', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('/api/events/evt-001/volunteers', () => {
        return HttpResponse.json(
          { error: 'Conflict', message: 'Volunteer already enrolled in this event' },
          { status: 409, headers: { 'X-Correlation-ID': 'test-corr-id' } },
        );
      }),
    );

    await renderEventDetail();

    // Wait for the Volunteers tab content to load
    await waitFor(() => {
      expect(screen.getByLabelText('Employee ID to enroll')).toBeInTheDocument();
    });

    // Type employee ID and submit
    const enrollInput = screen.getByLabelText('Employee ID to enroll');
    await user.type(enrollInput, 'EMP001');

    const enrollBtn = screen.getByRole('button', { name: /enroll/i });
    await user.click(enrollBtn);

    // Should display duplicate error
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/already enrolled/i)).toBeInTheDocument();
    });
  });

  it('shows capacity error when event is full', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('/api/events/evt-001/volunteers', () => {
        return HttpResponse.json(
          { error: 'Bad Request', message: 'Event is at full capacity' },
          { status: 400, headers: { 'X-Correlation-ID': 'test-corr-id' } },
        );
      }),
    );

    await renderEventDetail();

    await waitFor(() => {
      expect(screen.getByLabelText('Employee ID to enroll')).toBeInTheDocument();
    });

    const enrollInput = screen.getByLabelText('Employee ID to enroll');
    await user.type(enrollInput, 'EMP002');

    const enrollBtn = screen.getByRole('button', { name: /enroll/i });
    await user.click(enrollBtn);

    // Should display capacity error
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/capacity/i)).toBeInTheDocument();
    });
  });

  it('shows invalid status error when event cannot accept enrollments', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('/api/events/evt-001/volunteers', () => {
        return HttpResponse.json(
          { error: 'Bad Request', message: 'Cannot enroll volunteers: invalid event status' },
          { status: 400, headers: { 'X-Correlation-ID': 'test-corr-id' } },
        );
      }),
    );

    await renderEventDetail();

    await waitFor(() => {
      expect(screen.getByLabelText('Employee ID to enroll')).toBeInTheDocument();
    });

    const enrollInput = screen.getByLabelText('Employee ID to enroll');
    await user.type(enrollInput, 'EMP003');

    const enrollBtn = screen.getByRole('button', { name: /enroll/i });
    await user.click(enrollBtn);

    // Should display invalid status error
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/current event status/i)).toBeInTheDocument();
    });
  });
});
