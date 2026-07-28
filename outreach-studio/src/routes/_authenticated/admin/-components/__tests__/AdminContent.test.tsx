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

// Mock useToast to capture calls
const mockToastSuccess = vi.fn();
const mockToastError = vi.fn();
vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({
    toast: vi.fn(),
    success: mockToastSuccess,
    error: mockToastError,
    warning: vi.fn(),
    info: vi.fn(),
    dismiss: vi.fn(),
  }),
}));

// Mock Route.useSearch
vi.mock('../../index', () => ({
  Route: {
    useSearch: () => ({ page: 1, size: 10, role: undefined, status: undefined }),
  },
}));

// ---------------------------------------------------------------------------
// Mock data
// ---------------------------------------------------------------------------

const mockUsers = {
  content: [
    {
      id: 'user-001',
      username: 'priya_sharma',
      email: 'priya.sharma@company.com',
      role: 'ROLE_PMO',
      enabled: true,
    },
    {
      id: 'user-002',
      username: 'rahul_verma',
      email: 'rahul.verma@company.com',
      role: 'ROLE_POC',
      enabled: true,
    },
    {
      id: 'user-003',
      username: 'disabled_user',
      email: 'disabled@company.com',
      role: 'ROLE_POC',
      enabled: false,
    },
  ],
  totalElements: 3,
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
    http.get('/api/admin/users', () => {
      return HttpResponse.json(mockUsers);
    }),
    http.post('/api/admin/users', async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json(
        {
          id: 'user-new-001',
          username: body.username,
          email: body.email,
          role: body.role,
          enabled: true,
        },
        { status: 201 },
      );
    }),
    http.patch('/api/admin/users/:id/status', async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json({ status: body.status });
    }),
  );
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('AdminContent', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue(adminUser);
    mockToastSuccess.mockClear();
    mockToastError.mockClear();
    setupDefaultHandlers();
  });

  async function renderAdmin() {
    const { AdminContent } = await import('../AdminContent');
    return renderWithProviders(<AdminContent />);
  }

  // =========================================================================
  // User Creation Form Validation and Duplicate Error Handling
  // =========================================================================

  describe('User creation form validation and duplicate error handling', () => {
    it('shows create form when Create User button is clicked', async () => {
      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /Create User/i }));

      expect(screen.getByLabelText('Username')).toBeInTheDocument();
      expect(screen.getByLabelText('Email')).toBeInTheDocument();
      expect(screen.getByLabelText('Role')).toBeInTheDocument();
    });

    it('shows validation error for short username on blur', async () => {
      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /Create User/i }));

      const usernameInput = screen.getByLabelText('Username');
      await user.type(usernameInput, 'ab');
      await user.tab(); // trigger blur

      await waitFor(() => {
        expect(screen.getByText(/too_small|at least 3/i)).toBeInTheDocument();
      });
    });

    it('shows validation error for invalid email on blur', async () => {
      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /Create User/i }));

      const emailInput = screen.getByLabelText('Email');
      await user.type(emailInput, 'not-an-email');
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/invalid/i)).toBeInTheDocument();
      });
    });

    it('shows validation error for invalid username characters on blur', async () => {
      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /Create User/i }));

      const usernameInput = screen.getByLabelText('Username');
      await user.type(usernameInput, 'user@invalid!');
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/invalid/i)).toBeInTheDocument();
      });
    });

    it('disables submit button when form is invalid', async () => {
      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /Create User/i }));

      // Submit button should be disabled when fields are empty
      const submitBtn = screen.getByRole('button', { name: /Create$/i });
      expect(submitBtn).toBeDisabled();
    });

    it('enables submit button when all fields are valid', async () => {
      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /Create User/i }));

      await user.type(screen.getByLabelText('Username'), 'new_user');
      await user.type(screen.getByLabelText('Email'), 'new@example.com');

      await waitFor(() => {
        const submitBtn = screen.getByRole('button', { name: /Create$/i });
        expect(submitBtn).not.toBeDisabled();
      });
    });

    it('displays duplicate username error from server', async () => {
      server.use(
        http.post('/api/admin/users', () => {
          return HttpResponse.json(
            {
              error: 'Conflict',
              message: 'Username already exists',
              fieldErrors: [
                { field: 'username', message: 'A user with this username already exists' },
              ],
            },
            { status: 409, headers: { 'X-Correlation-ID': 'test-id' } },
          );
        }),
      );

      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /Create User/i }));

      await user.type(screen.getByLabelText('Username'), 'existing_user');
      await user.type(screen.getByLabelText('Email'), 'new@example.com');

      const submitBtn = screen.getByRole('button', { name: /Create$/i });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText('A user with this username already exists')).toBeInTheDocument();
      });
    });

    it('displays duplicate email error from server', async () => {
      server.use(
        http.post('/api/admin/users', () => {
          return HttpResponse.json(
            {
              error: 'Conflict',
              message: 'Email already exists',
              fieldErrors: [
                { field: 'email', message: 'A user with this email already exists' },
              ],
            },
            { status: 409, headers: { 'X-Correlation-ID': 'test-id' } },
          );
        }),
      );

      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /Create User/i }));

      await user.type(screen.getByLabelText('Username'), 'new_user');
      await user.type(screen.getByLabelText('Email'), 'existing@example.com');

      const submitBtn = screen.getByRole('button', { name: /Create$/i });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText('A user with this email already exists')).toBeInTheDocument();
      });
    });

    it('shows generic error toast when server returns non-field error', async () => {
      server.use(
        http.post('/api/admin/users', () => {
          return HttpResponse.json(
            {
              error: 'Internal Server Error',
              message: 'An unexpected error occurred',
              fieldErrors: [],
            },
            { status: 500, headers: { 'X-Correlation-ID': 'test-id' } },
          );
        }),
      );

      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /Create User/i }));

      await user.type(screen.getByLabelText('Username'), 'new_user');
      await user.type(screen.getByLabelText('Email'), 'new@example.com');

      const submitBtn = screen.getByRole('button', { name: /Create$/i });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(mockToastError).toHaveBeenCalled();
      });
    });
  });

  // =========================================================================
  // Confirmation Dialog for Disable/Enable Actions
  // =========================================================================

  describe('Confirmation dialog for disable/enable actions', () => {
    it('shows confirmation dialog when Disable button is clicked', async () => {
      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      // Click the Disable button for the first enabled user
      const disableBtn = screen.getByRole('button', { name: /Disable account for priya_sharma/i });
      await user.click(disableBtn);

      // Confirmation dialog should appear
      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      expect(screen.getByText(/Are you sure you want to disable the account for "priya_sharma"/i)).toBeInTheDocument();
    });

    it('shows confirmation dialog when Enable button is clicked', async () => {
      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('disabled_user')).toBeInTheDocument();
      });

      // Click Enable button for the disabled user
      const enableBtn = screen.getByRole('button', { name: /Enable account for disabled_user/i });
      await user.click(enableBtn);

      // Confirmation dialog should appear
      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      expect(screen.getByText(/Are you sure you want to enable/i)).toBeInTheDocument();
    });

    it('calls status change API when confirm button is clicked', async () => {
      let statusPayload: Record<string, unknown> | null = null;
      server.use(
        http.patch('/api/admin/users/:id/status', async ({ request }) => {
          statusPayload = (await request.json()) as Record<string, unknown>;
          return HttpResponse.json({ status: 'DISABLED' });
        }),
      );

      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      const disableBtn = screen.getByRole('button', { name: /Disable account for priya_sharma/i });
      await user.click(disableBtn);

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      // Click Confirm (the Disable button in the dialog)
      const confirmBtn = screen.getByRole('button', { name: /^Disable$/i });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(statusPayload).not.toBeNull();
      });

      expect(statusPayload!.status).toBe('DISABLED');
    });

    it('closes dialog without action when Cancel is clicked', async () => {
      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      const disableBtn = screen.getByRole('button', { name: /Disable account for priya_sharma/i });
      await user.click(disableBtn);

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      // Click Cancel
      await user.click(screen.getByRole('button', { name: /Cancel/i }));

      await waitFor(() => {
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
      });
    });

    it('shows success toast after successful status change', async () => {
      const user = userEvent.setup();
      await renderAdmin();

      await waitFor(() => {
        expect(screen.getByText('priya_sharma')).toBeInTheDocument();
      });

      const disableBtn = screen.getByRole('button', { name: /Disable account for priya_sharma/i });
      await user.click(disableBtn);

      await waitFor(() => {
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
      });

      const confirmBtn = screen.getByRole('button', { name: /^Disable$/i });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(mockToastSuccess).toHaveBeenCalledWith('Account status updated successfully');
      });
    });
  });
});
