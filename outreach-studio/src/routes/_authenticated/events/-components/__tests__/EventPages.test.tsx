import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import { buildEvent } from '@/test/factories/event.factory';
import type { Event } from '@/types/domain';
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

vi.mock('../../$eventId', () => ({
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

function createEventPage(events: Event[], page = 0, size = 10): PageResponse<Event> {
  return {
    content: events,
    totalElements: events.length,
    totalPages: Math.ceil(events.length / size),
    page,
    size,
  };
}

// ---------------------------------------------------------------------------
// Event List Tests
// ---------------------------------------------------------------------------

describe('EventListContent', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue(adminUser);
    mockSearchParams.mockReturnValue({ page: 1, size: 10, search: undefined, status: undefined });
  });

  async function renderEventList() {
    const { EventListContent } = await import('../EventListContent');
    return renderWithProviders(<EventListContent />);
  }

  describe('renders with mocked paginated data', () => {
    it('displays event rows from the API response', async () => {
      const events = [
        buildEvent({ id: 'e1', eventName: 'Community Cleanup', eventCode: 'CC001', status: 'ACTIVE', city: 'Mumbai' }),
        buildEvent({ id: 'e2', eventName: 'Tech Workshop', eventCode: 'TW002', status: 'DRAFT', city: 'Bangalore' }),
      ];

      server.use(
        http.get('/api/events', () => {
          return HttpResponse.json(createEventPage(events));
        }),
      );

      await renderEventList();

      await waitFor(() => {
        expect(screen.getByText('Community Cleanup')).toBeInTheDocument();
      });
      expect(screen.getByText('Tech Workshop')).toBeInTheDocument();
      expect(screen.getByText('CC001')).toBeInTheDocument();
      expect(screen.getByText('TW002')).toBeInTheDocument();
      expect(screen.getByText('ACTIVE')).toBeInTheDocument();
      expect(screen.getByText('DRAFT')).toBeInTheDocument();
    });

    it('renders the Create Event link', async () => {
      server.use(
        http.get('/api/events', () => {
          return HttpResponse.json(createEventPage([buildEvent()]));
        }),
      );

      await renderEventList();

      await waitFor(() => {
        expect(screen.getByText('Create Event')).toBeInTheDocument();
      });
    });

    it('renders pagination info', async () => {
      const events = Array.from({ length: 10 }, (_, i) =>
        buildEvent({ id: `e${i}`, eventName: `Event ${i + 1}` }),
      );

      server.use(
        http.get('/api/events', () => {
          return HttpResponse.json({
            content: events,
            totalElements: 25,
            totalPages: 3,
            page: 0,
            size: 10,
          });
        }),
      );

      await renderEventList();

      await waitFor(() => {
        expect(screen.getByText(/25 total records/)).toBeInTheDocument();
      });
    });

    it('shows empty message when no events are returned', async () => {
      server.use(
        http.get('/api/events', () => {
          return HttpResponse.json(createEventPage([]));
        }),
      );

      await renderEventList();

      await waitFor(() => {
        expect(screen.getByText('No events found.')).toBeInTheDocument();
      });
    });
  });
});

// ---------------------------------------------------------------------------
// Event Create Tests
// ---------------------------------------------------------------------------

describe('EventCreateContent', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue(adminUser);
    mockNavigate.mockClear();
  });

  async function renderEventCreate() {
    const { EventCreateContent } = await import('../EventCreateContent');
    return renderWithProviders(<EventCreateContent />);
  }

  describe('form validation on blur', () => {
    it('shows error when name field is left empty on blur', async () => {
      const user = userEvent.setup();
      await renderEventCreate();

      const nameInput = screen.getByLabelText('Name');
      await user.click(nameInput);
      await user.tab(); // blur

      await waitFor(() => {
        expect(screen.getByText(/String must contain at least 1/i)).toBeInTheDocument();
      });
    });

    it('shows error when end date is before start date', async () => {
      const user = userEvent.setup();
      await renderEventCreate();

      const startDateInput = screen.getByLabelText('Event Date');
      const endDateInput = screen.getByLabelText('End Date');

      await user.type(startDateInput, '2025-06-15');
      await user.type(endDateInput, '2025-06-10');
      await user.tab(); // blur

      await waitFor(() => {
        expect(screen.getByText(/End date must be on or after start date/i)).toBeInTheDocument();
      });
    });

    it('shows error for empty required fields on blur', async () => {
      const user = userEvent.setup();
      await renderEventCreate();

      const cityInput = screen.getByLabelText('City');
      await user.click(cityInput);
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/String must contain at least 1/i)).toBeInTheDocument();
      });
    });
  });

  describe('server errors mapped to form fields', () => {
    it('maps server field-level validation errors to corresponding form fields', async () => {
      const user = userEvent.setup();

      server.use(
        http.post('/api/events', () => {
          return HttpResponse.json(
            {
              error: 'VALIDATION_ERROR',
              message: 'Validation failed',
              correlationId: 'corr-123',
              fieldErrors: [
                { field: 'eventName', message: 'Event name already exists' },
                { field: 'city', message: 'City is not supported' },
              ],
            },
            { status: 400 },
          );
        }),
      );

      await renderEventCreate();

      // Fill all fields to enable submit
      await user.type(screen.getByLabelText('Name'), 'Test Event');
      await user.type(screen.getByLabelText('Description'), 'A test description');
      await user.type(screen.getByLabelText('Event Date'), '2025-06-01');
      await user.type(screen.getByLabelText('End Date'), '2025-06-05');
      await user.type(screen.getByLabelText('City'), 'Mumbai');
      await user.type(screen.getByLabelText('Venue'), 'Hall A');
      await user.type(screen.getByLabelText('Category'), 'CSR');
      await user.clear(screen.getByLabelText('Max Volunteers'));
      await user.type(screen.getByLabelText('Max Volunteers'), '50');

      // Submit the form
      const submitBtn = screen.getByRole('button', { name: /Create Event/i });
      await user.click(submitBtn);

      // Wait for server error to be mapped to fields
      await waitFor(() => {
        expect(screen.getByText('Event name already exists')).toBeInTheDocument();
      });
      expect(screen.getByText('City is not supported')).toBeInTheDocument();
    });

    it('shows general server error when no field errors provided', async () => {
      const user = userEvent.setup();

      server.use(
        http.post('/api/events', () => {
          return HttpResponse.json(
            {
              error: 'INTERNAL_ERROR',
              message: 'An unexpected error occurred',
              correlationId: 'corr-456',
              fieldErrors: [],
            },
            { status: 500 },
          );
        }),
      );

      await renderEventCreate();

      await user.type(screen.getByLabelText('Name'), 'Test Event');
      await user.type(screen.getByLabelText('Description'), 'A test description');
      await user.type(screen.getByLabelText('Event Date'), '2025-06-01');
      await user.type(screen.getByLabelText('End Date'), '2025-06-05');
      await user.type(screen.getByLabelText('City'), 'Mumbai');
      await user.type(screen.getByLabelText('Venue'), 'Hall A');
      await user.type(screen.getByLabelText('Category'), 'CSR');
      await user.clear(screen.getByLabelText('Max Volunteers'));
      await user.type(screen.getByLabelText('Max Volunteers'), '50');

      const submitBtn = screen.getByRole('button', { name: /Create Event/i });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText('An unexpected error occurred')).toBeInTheDocument();
      });
    });
  });
});

// ---------------------------------------------------------------------------
// Event Detail Tests — Lifecycle Transitions
// ---------------------------------------------------------------------------

describe('EventDetailContent', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue(adminUser);
    mockRouteParams.mockReturnValue({ eventId: 'evt-001' });
    mockSearchParams.mockReturnValue({ tab: 'overview' });
    mockNavigate.mockClear();
  });

  async function renderEventDetail() {
    const { EventDetailContent } = await import('../EventDetailContent');
    return renderWithProviders(<EventDetailContent />);
  }

  function setupEventDetailHandler(eventOverrides: Partial<Event> = {}) {
    const eventData = {
      id: 'evt-001',
      eventCode: 'EVT2024001',
      eventName: 'Annual Volunteer Drive',
      description: 'A large-scale volunteer initiative.',
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
      createdBy: 'Priya Sharma',
      ...eventOverrides,
    };

    server.use(
      http.get('/api/events/:eventId', () => {
        return HttpResponse.json(eventData);
      }),
    );

    return eventData;
  }

  describe('valid transition button visibility per status', () => {
    it('shows Activate and Cancel buttons for PUBLISHED status', async () => {
      setupEventDetailHandler({ status: 'PUBLISHED' });

      await renderEventDetail();

      await waitFor(() => {
        expect(screen.getByText('Annual Volunteer Drive')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /Activate/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Publish/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Complete/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Archive/i })).not.toBeInTheDocument();
    });

    it('shows Publish and Cancel buttons for DRAFT status', async () => {
      setupEventDetailHandler({ status: 'DRAFT' });

      await renderEventDetail();

      await waitFor(() => {
        expect(screen.getByText('Annual Volunteer Drive')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /Publish/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Activate/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Complete/i })).not.toBeInTheDocument();
    });

    it('shows Complete button for ACTIVE status', async () => {
      setupEventDetailHandler({ status: 'ACTIVE' });

      await renderEventDetail();

      await waitFor(() => {
        expect(screen.getByText('Annual Volunteer Drive')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /Complete/i })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Publish/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Cancel/i })).not.toBeInTheDocument();
    });

    it('shows Archive button for COMPLETED status', async () => {
      setupEventDetailHandler({ status: 'COMPLETED' });

      await renderEventDetail();

      await waitFor(() => {
        expect(screen.getByText('Annual Volunteer Drive')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /Archive/i })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Complete/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Cancel/i })).not.toBeInTheDocument();
    });

    it('shows no transition buttons for ARCHIVED status', async () => {
      setupEventDetailHandler({ status: 'ARCHIVED' });

      await renderEventDetail();

      await waitFor(() => {
        expect(screen.getByText('Annual Volunteer Drive')).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: /Publish/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Activate/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Complete/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Archive/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Cancel/i })).not.toBeInTheDocument();
    });

    it('shows no transition buttons for CANCELLED status', async () => {
      setupEventDetailHandler({ status: 'CANCELLED' });

      await renderEventDetail();

      await waitFor(() => {
        expect(screen.getByText('Annual Volunteer Drive')).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: /Publish/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Activate/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Complete/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Archive/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Cancel/i })).not.toBeInTheDocument();
    });
  });

  describe('lifecycle transition optimistic update and rollback', () => {
    it('applies optimistic status update immediately on transition click', async () => {
      const user = userEvent.setup();
      let patchCalled = false;

      setupEventDetailHandler({ status: 'PUBLISHED' });

      server.use(
        http.patch('/api/events/:eventId/status', async () => {
          patchCalled = true;
          // Simulate slow response
          await new Promise((resolve) => setTimeout(resolve, 200));
          return HttpResponse.json({
            id: 'evt-001',
            eventCode: 'EVT2024001',
            eventName: 'Annual Volunteer Drive',
            description: 'A large-scale volunteer initiative.',
            status: 'ACTIVE',
            eventDate: '2024-06-01',
            eventEndDate: '2024-06-03',
            city: 'Mumbai',
            venue: 'Convention Center',
            category: 'CSR',
            maxVolunteers: 200,
            registeredCount: 45,
            attendedCount: 38,
            createdAt: '2024-05-01T10:00:00Z',
            updatedAt: new Date().toISOString(),
            createdBy: 'Priya Sharma',
          });
        }),
      );

      await renderEventDetail();

      await waitFor(() => {
        expect(screen.getByText('Annual Volunteer Drive')).toBeInTheDocument();
      });

      // Click Activate button
      await user.click(screen.getByRole('button', { name: /Activate/i }));

      // Optimistic update: status badge should show ACTIVE immediately
      // The badge is the first match; detail grid also shows status text
      await waitFor(() => {
        expect(screen.getAllByText('ACTIVE').length).toBeGreaterThanOrEqual(1);
      });
      expect(patchCalled).toBe(true);
    });

    it('rolls back status on server rejection and shows error', async () => {
      const user = userEvent.setup();

      setupEventDetailHandler({ status: 'PUBLISHED' });

      server.use(
        http.patch('/api/events/:eventId/status', () => {
          return HttpResponse.json(
            {
              error: 'Bad Request',
              message: 'Invalid status transition',
              correlationId: 'corr-789',
              fieldErrors: [],
            },
            { status: 400 },
          );
        }),
      );

      await renderEventDetail();

      await waitFor(() => {
        expect(screen.getByText('Annual Volunteer Drive')).toBeInTheDocument();
      });

      // Confirm initial PUBLISHED status is displayed in the badge
      const headerSection = screen.getByText('Annual Volunteer Drive').closest('div')!.parentElement!;
      expect(headerSection).toHaveTextContent('PUBLISHED');

      // Click Activate button
      await user.click(screen.getByRole('button', { name: /Activate/i }));

      // After server rejection, status should roll back to PUBLISHED and error shown
      await waitFor(() => {
        expect(headerSection).toHaveTextContent('PUBLISHED');
      });

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });
    });
  });
});
