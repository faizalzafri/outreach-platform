import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
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

// ---------------------------------------------------------------------------
// Mock data
// ---------------------------------------------------------------------------

const mockTemplates = {
  content: [
    {
      id: 'tpl-001',
      name: 'Event Registration Confirmation',
      type: 'EMAIL',
      subjectTemplate: 'You are registered for {{eventName}}',
      bodyTemplate: '<p>You are registered for {{eventName}}.</p>',
      engine: 'THYMELEAF',
      active: true,
      version: 3,
      variablesSchema: JSON.stringify({ type: 'object', properties: { eventName: { type: 'string' } } }),
      createdAt: '2024-01-15T10:00:00Z',
      updatedAt: '2024-05-20T16:00:00Z',
    },
    {
      id: 'tpl-002',
      name: 'Event Reminder SMS',
      type: 'SMS',
      subjectTemplate: '',
      bodyTemplate: 'Reminder: {{eventName}} is tomorrow.',
      engine: 'THYMELEAF',
      active: false,
      version: 1,
      variablesSchema: JSON.stringify({ type: 'object', properties: { eventName: { type: 'string' } } }),
      createdAt: '2024-05-01T08:00:00Z',
      updatedAt: '2024-05-01T08:00:00Z',
    },
  ],
  totalElements: 2,
  totalPages: 1,
  page: 0,
  size: 10,
};

const mockDeliveryRecords = {
  content: [
    {
      id: 'del-001',
      recipient: 'Anita Desai',
      eventName: 'Annual Volunteer Drive',
      status: 'DELIVERED',
      timestamp: '2024-06-01T08:00:00Z',
    },
    {
      id: 'del-002',
      recipient: 'Vikram Singh',
      eventName: 'Annual Volunteer Drive',
      status: 'FAILED',
      timestamp: '2024-06-01T08:00:00Z',
    },
    {
      id: 'del-003',
      recipient: 'Priya Kumar',
      eventName: 'Charity Gala',
      status: 'BOUNCED',
      timestamp: '2024-06-02T09:30:00Z',
    },
    {
      id: 'del-004',
      recipient: 'Raj Patel',
      eventName: 'Community Cleanup',
      status: 'SENT',
      timestamp: '2024-06-03T07:00:00Z',
    },
    {
      id: 'del-005',
      recipient: 'Meera Nair',
      eventName: 'Blood Drive',
      status: 'PENDING',
      timestamp: '2024-06-03T10:00:00Z',
    },
  ],
  totalElements: 5,
  totalPages: 1,
  page: 0,
  size: 10,
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
    http.get('/api/notifications/templates', () => {
      return HttpResponse.json(mockTemplates);
    }),
    http.get('/api/notifications/history', () => {
      return HttpResponse.json(mockDeliveryRecords);
    }),
    http.post('/api/notifications/templates/:id/preview', () => {
      return HttpResponse.json({
        renderedSubject: 'You are registered for Annual Volunteer Drive',
        renderedBody: '<h1>Hello Anita!</h1><p>You are registered for Annual Volunteer Drive.</p>',
      });
    }),
    http.post('/api/notifications/retry/:eventId', () => {
      return HttpResponse.json({ retriedCount: 1, status: 'RETRYING' });
    }),
    http.post('/api/notifications/schedules', () => {
      return HttpResponse.json({ id: 'sched-001', status: 'SCHEDULED' }, { status: 201 });
    }),
  );
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('NotificationsContent', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue(adminUser);
    mockToastError.mockClear();
    setupDefaultHandlers();
  });

  async function renderNotifications() {
    const { NotificationsContent } = await import('../NotificationsContent');
    return renderWithProviders(<NotificationsContent />);
  }

  // =========================================================================
  // Template Preview Modal
  // =========================================================================

  describe('Template preview modal', () => {
    it('opens preview modal with rendered content on success', async () => {
      const user = userEvent.setup();
      await renderNotifications();

      // Wait for templates to load
      await waitFor(() => {
        expect(screen.getByText('Event Registration Confirmation')).toBeInTheDocument();
      });

      // Click Preview button on first template
      const previewButtons = screen.getAllByRole('button', { name: /Preview/i });
      await user.click(previewButtons[0]!);

      // Preview modal should open with rendered content
      await waitFor(() => {
        expect(screen.getByRole('dialog', { name: /Template preview/i })).toBeInTheDocument();
      });

      expect(screen.getByText('Template Preview')).toBeInTheDocument();
      expect(screen.getByText('You are registered for Annual Volunteer Drive')).toBeInTheDocument();
    });

    it('closes preview modal when Close button is clicked', async () => {
      const user = userEvent.setup();
      await renderNotifications();

      await waitFor(() => {
        expect(screen.getByText('Event Registration Confirmation')).toBeInTheDocument();
      });

      const previewButtons = screen.getAllByRole('button', { name: /Preview/i });
      await user.click(previewButtons[0]!);

      await waitFor(() => {
        expect(screen.getByRole('dialog', { name: /Template preview/i })).toBeInTheDocument();
      });

      // Close the modal
      await user.click(screen.getByRole('button', { name: /Close/i }));

      await waitFor(() => {
        expect(screen.queryByRole('dialog', { name: /Template preview/i })).not.toBeInTheDocument();
      });
    });

    it('shows error toast and does not open modal on preview failure', async () => {
      server.use(
        http.post('/api/notifications/templates/:id/preview', () => {
          return HttpResponse.json(
            { message: 'Template rendering failed: missing required variable "eventName"' },
            { status: 400 },
          );
        }),
      );

      const user = userEvent.setup();
      await renderNotifications();

      await waitFor(() => {
        expect(screen.getByText('Event Registration Confirmation')).toBeInTheDocument();
      });

      const previewButtons = screen.getAllByRole('button', { name: /Preview/i });
      await user.click(previewButtons[0]!);

      // Wait for the error toast to be called
      await waitFor(() => {
        expect(mockToastError).toHaveBeenCalled();
      });

      // Preview modal should NOT be open
      expect(screen.queryByRole('dialog', { name: /Template preview/i })).not.toBeInTheDocument();
    });
  });

  // =========================================================================
  // Retry Button Visibility Logic
  // =========================================================================

  describe('Retry button visibility logic', () => {
    it('shows Retry button only for Failed and Bounced records', async () => {
      const user = userEvent.setup();
      await renderNotifications();

      // Switch to Delivery tab
      await user.click(screen.getByRole('tab', { name: /Delivery/i }));

      // Wait for delivery records to load
      await waitFor(() => {
        expect(screen.getByText('Anita Desai')).toBeInTheDocument();
      });

      // Get all Retry buttons - should only appear for FAILED and BOUNCED
      const retryButtons = screen.getAllByRole('button', { name: /Retry/i });
      expect(retryButtons).toHaveLength(2); // del-002 (FAILED) and del-003 (BOUNCED)
    });

    it('does not show Retry button for DELIVERED status', async () => {
      const user = userEvent.setup();
      server.use(
        http.get('/api/notifications/history', () => {
          return HttpResponse.json({
            content: [
              {
                id: 'del-001',
                recipient: 'Anita Desai',
                eventName: 'Annual Volunteer Drive',
                status: 'DELIVERED',
                timestamp: '2024-06-01T08:00:00Z',
              },
            ],
            totalElements: 1,
            totalPages: 1,
            page: 0,
            size: 10,
          });
        }),
      );

      await renderNotifications();
      await user.click(screen.getByRole('tab', { name: /Delivery/i }));

      await waitFor(() => {
        expect(screen.getByText('Anita Desai')).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: /Retry/i })).not.toBeInTheDocument();
    });

    it('does not show Retry button for PENDING status', async () => {
      const user = userEvent.setup();
      server.use(
        http.get('/api/notifications/history', () => {
          return HttpResponse.json({
            content: [
              {
                id: 'del-005',
                recipient: 'Meera Nair',
                eventName: 'Blood Drive',
                status: 'PENDING',
                timestamp: '2024-06-03T10:00:00Z',
              },
            ],
            totalElements: 1,
            totalPages: 1,
            page: 0,
            size: 10,
          });
        }),
      );

      await renderNotifications();
      await user.click(screen.getByRole('tab', { name: /Delivery/i }));

      await waitFor(() => {
        expect(screen.getByText('Meera Nair')).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: /Retry/i })).not.toBeInTheDocument();
    });

    it('does not show Retry button for SENT status', async () => {
      const user = userEvent.setup();
      server.use(
        http.get('/api/notifications/history', () => {
          return HttpResponse.json({
            content: [
              {
                id: 'del-004',
                recipient: 'Raj Patel',
                eventName: 'Community Cleanup',
                status: 'SENT',
                timestamp: '2024-06-03T07:00:00Z',
              },
            ],
            totalElements: 1,
            totalPages: 1,
            page: 0,
            size: 10,
          });
        }),
      );

      await renderNotifications();
      await user.click(screen.getByRole('tab', { name: /Delivery/i }));

      await waitFor(() => {
        expect(screen.getByText('Raj Patel')).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: /Retry/i })).not.toBeInTheDocument();
    });
  });

  // =========================================================================
  // Cron Validation for Scheduled Notifications
  // =========================================================================

  describe('Cron validation for scheduled notifications', () => {
    it('shows cron field only when Scheduled trigger type is selected', async () => {
      const user = userEvent.setup();
      await renderNotifications();

      // Switch to Schedule tab
      await user.click(screen.getByRole('tab', { name: /Schedule/i }));

      await waitFor(() => {
        expect(screen.getByLabelText('Trigger Type')).toBeInTheDocument();
      });

      // Cron field should not be visible for IMMEDIATE (default)
      expect(screen.queryByLabelText('Cron Expression')).not.toBeInTheDocument();

      // Select Scheduled trigger type
      await user.selectOptions(screen.getByLabelText('Trigger Type'), 'SCHEDULED');

      // Cron field should now be visible
      expect(screen.getByLabelText('Cron Expression')).toBeInTheDocument();
    });

    it('shows validation error for invalid cron expression on submit', async () => {
      const user = userEvent.setup();
      await renderNotifications();

      await user.click(screen.getByRole('tab', { name: /Schedule/i }));

      await waitFor(() => {
        expect(screen.getByLabelText('Trigger Type')).toBeInTheDocument();
      });

      // Fill out required fields
      await user.selectOptions(screen.getByLabelText('Template'), 'tpl-001');
      await user.type(screen.getByLabelText('Target Event'), 'evt-001');
      await user.selectOptions(screen.getByLabelText('Trigger Type'), 'SCHEDULED');

      // Enter invalid cron expression
      await user.type(screen.getByLabelText('Cron Expression'), 'invalid cron');

      // Submit the form
      await user.click(screen.getByRole('button', { name: /Create Schedule/i }));

      // Validation error should appear
      await waitFor(() => {
        expect(screen.getByText(/Invalid cron expression/)).toBeInTheDocument();
      });
    });

    it('accepts valid 5-field cron expression', async () => {
      const user = userEvent.setup();
      let schedulePayload: Record<string, unknown> | null = null;

      server.use(
        http.post('/api/notifications/schedules', async ({ request }) => {
          schedulePayload = (await request.json()) as Record<string, unknown>;
          return HttpResponse.json({ id: 'sched-001', status: 'SCHEDULED' }, { status: 201 });
        }),
      );

      await renderNotifications();

      await user.click(screen.getByRole('tab', { name: /Schedule/i }));

      await waitFor(() => {
        expect(screen.getByLabelText('Trigger Type')).toBeInTheDocument();
      });

      // Fill out required fields
      await user.selectOptions(screen.getByLabelText('Template'), 'tpl-001');
      await user.type(screen.getByLabelText('Target Event'), 'evt-001');
      await user.selectOptions(screen.getByLabelText('Trigger Type'), 'SCHEDULED');
      await user.type(screen.getByLabelText('Cron Expression'), '0 9 * * 1');

      // Submit
      await user.click(screen.getByRole('button', { name: /Create Schedule/i }));

      // Should submit successfully without cron error
      await waitFor(() => {
        expect(schedulePayload).not.toBeNull();
      });

      expect(schedulePayload!.cronExpression).toBe('0 9 * * 1');
      expect(screen.queryByText(/Invalid cron expression/)).not.toBeInTheDocument();
    });

    it('shows validation error for cron with wrong number of fields', async () => {
      const user = userEvent.setup();
      await renderNotifications();

      await user.click(screen.getByRole('tab', { name: /Schedule/i }));

      await waitFor(() => {
        expect(screen.getByLabelText('Trigger Type')).toBeInTheDocument();
      });

      await user.selectOptions(screen.getByLabelText('Template'), 'tpl-001');
      await user.type(screen.getByLabelText('Target Event'), 'evt-001');
      await user.selectOptions(screen.getByLabelText('Trigger Type'), 'SCHEDULED');

      // Enter cron with only 3 fields (invalid)
      await user.type(screen.getByLabelText('Cron Expression'), '0 9 *');

      await user.click(screen.getByRole('button', { name: /Create Schedule/i }));

      await waitFor(() => {
        expect(screen.getByText(/Invalid cron expression/)).toBeInTheDocument();
      });
    });

    it('hides cron field when switching away from Scheduled trigger type', async () => {
      const user = userEvent.setup();
      await renderNotifications();

      await user.click(screen.getByRole('tab', { name: /Schedule/i }));

      await waitFor(() => {
        expect(screen.getByLabelText('Trigger Type')).toBeInTheDocument();
      });

      // Select Scheduled, then switch to Immediate
      await user.selectOptions(screen.getByLabelText('Trigger Type'), 'SCHEDULED');
      expect(screen.getByLabelText('Cron Expression')).toBeInTheDocument();

      await user.selectOptions(screen.getByLabelText('Trigger Type'), 'IMMEDIATE');
      expect(screen.queryByLabelText('Cron Expression')).not.toBeInTheDocument();
    });
  });
});
