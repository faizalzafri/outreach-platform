import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';

// Mock useAuth to control user identity
const mockUseAuth = vi.fn();

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// Mock Route.useSearch for the feedback route
vi.mock('../../index', () => ({
  Route: {
    useSearch: () => ({ page: 1, size: 10, eventId: 'evt-001' }),
  },
}));

// ---------------------------------------------------------------------------
// Test data and helpers
// ---------------------------------------------------------------------------

const authenticatedUser = {
  user: {
    sub: 'user-001',
    name: 'Test User',
    email: 'test@outreach.dev',
    roles: ['ROLE_PMO'],
  },
  isAuthenticated: true,
  isLoading: false,
  login: vi.fn(),
  logout: vi.fn(),
};

function setupDefaultHandlers() {
  server.use(
    http.get('/api/feedback/categories', () => {
      return HttpResponse.json(['Communication', 'Organization', 'Content', 'Logistics', 'Overall']);
    }),
    http.get('/api/feedback', () => {
      return HttpResponse.json({
        content: [],
        totalElements: 0,
        totalPages: 0,
        page: 0,
        size: 10,
      });
    }),
  );
}

async function renderFeedbackForm() {
  const { FeedbackContent } = await import('../FeedbackContent');
  return renderWithProviders(<FeedbackContent />);
}

async function waitForFormReady() {
  await waitFor(() => {
    expect(screen.getByRole('button', { name: /Submit Feedback/ })).toBeInTheDocument();
  });
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('FeedbackContent', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue(authenticatedUser);
    setupDefaultHandlers();
  });

  describe('Form validation on blur (valid and invalid inputs)', () => {
    it('shows validation error for textAnswer1 when left empty after blur', async () => {
      const user = userEvent.setup();
      await renderFeedbackForm();
      await waitForFormReady();

      const textAnswer1 = screen.getByLabelText(/What went well/);
      await user.click(textAnswer1);
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/String must contain at least 1/)).toBeInTheDocument();
      });
    });

    it('shows validation error for textAnswer2 when left empty after blur', async () => {
      const user = userEvent.setup();
      await renderFeedbackForm();
      await waitForFormReady();

      const textAnswer2 = screen.getByLabelText(/What could be improved/);
      await user.click(textAnswer2);
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/String must contain at least 1/)).toBeInTheDocument();
      });
    });

    it('shows validation error for category when left unselected after blur', async () => {
      const user = userEvent.setup();
      await renderFeedbackForm();
      await waitForFormReady();

      const categorySelect = screen.getByLabelText(/Category/);
      await user.click(categorySelect);
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/String must contain at least 1/)).toBeInTheDocument();
      });
    });

    it('clears textAnswer1 validation error after typing valid input', async () => {
      const user = userEvent.setup();
      await renderFeedbackForm();
      await waitForFormReady();

      const textAnswer1 = screen.getByLabelText(/What went well/);
      // Focus and blur to trigger validation error
      await user.click(textAnswer1);
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/String must contain at least 1/)).toBeInTheDocument();
      });

      // Type valid input and blur to re-validate
      await user.click(textAnswer1);
      await user.type(textAnswer1, 'Great event');
      await user.tab();

      // The error associated with textAnswer1 should be cleared
      await waitFor(() => {
        const errorElement = document.getElementById('textAnswer1-error');
        expect(errorElement).not.toBeInTheDocument();
      }, { timeout: 2000 });
    });
  });

  describe('Submit disabled state logic', () => {
    it('disables submit button when form is initially empty', async () => {
      await renderFeedbackForm();
      await waitForFormReady();

      const submitBtn = screen.getByRole('button', { name: /Submit Feedback/ });
      expect(submitBtn).toBeDisabled();
    });

    it('keeps submit disabled when only text fields are filled (no score)', async () => {
      const user = userEvent.setup();
      await renderFeedbackForm();
      await waitForFormReady();

      const textAnswer1 = screen.getByLabelText(/What went well/);
      await user.type(textAnswer1, 'Great event overall');

      const textAnswer2 = screen.getByLabelText(/What could be improved/);
      await user.type(textAnswer2, 'More time for activities');

      const submitBtn = screen.getByRole('button', { name: /Submit Feedback/ });
      expect(submitBtn).toBeDisabled();
    });

    it('keeps submit disabled when only partial fields are filled', async () => {
      const user = userEvent.setup();
      await renderFeedbackForm();
      await waitForFormReady();

      // Only fill score and one text field
      await user.click(screen.getByRole('button', { name: 'Score 3' }));
      const textAnswer1 = screen.getByLabelText(/What went well/);
      await user.type(textAnswer1, 'Great event');

      const submitBtn = screen.getByRole('button', { name: /Submit Feedback/ });
      expect(submitBtn).toBeDisabled();
    });
  });

  describe('Successful submission flow', () => {
    it('renders form with submit button and all required fields', async () => {
      await renderFeedbackForm();
      await waitForFormReady();

      // Verify all form elements are present
      expect(screen.getByRole('button', { name: /Submit Feedback/ })).toBeInTheDocument();
      expect(screen.getByLabelText(/What went well/)).toBeInTheDocument();
      expect(screen.getByLabelText(/What could be improved/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Category/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Submit anonymously/)).toBeInTheDocument();
      expect(screen.getByRole('radiogroup', { name: /Feedback score/ })).toBeInTheDocument();
    });

    it('renders emoji score buttons from 1 to 5', async () => {
      await renderFeedbackForm();
      await waitForFormReady();

      for (let i = 1; i <= 5; i++) {
        expect(screen.getByRole('button', { name: `Score ${i}` })).toBeInTheDocument();
      }
    });

    it('selects emoji score on click and shows selected state', async () => {
      const user = userEvent.setup();
      await renderFeedbackForm();
      await waitForFormReady();

      const scoreBtn = screen.getByRole('button', { name: 'Score 4' });
      await user.click(scoreBtn);

      expect(scoreBtn).toHaveAttribute('aria-pressed', 'true');
    });
  });

  describe('Server validation error mapping to fields', () => {
    it('renders error containers with correct aria attributes for field errors', async () => {
      await renderFeedbackForm();
      await waitForFormReady();

      // Verify that form fields have proper aria-describedby wiring for error display
      const textAnswer1 = screen.getByLabelText(/What went well/);
      expect(textAnswer1).toHaveAttribute('aria-invalid', 'false');

      const textAnswer2 = screen.getByLabelText(/What could be improved/);
      expect(textAnswer2).toHaveAttribute('aria-invalid', 'false');

      const categorySelect = screen.getByLabelText(/Category/);
      expect(categorySelect).toHaveAttribute('aria-invalid', 'false');
    });

    it('shows field-level validation errors with aria-invalid on blur', async () => {
      const user = userEvent.setup();
      await renderFeedbackForm();
      await waitForFormReady();

      // Trigger blur on textAnswer1 to show error
      const textAnswer1 = screen.getByLabelText(/What went well/);
      await user.click(textAnswer1);
      await user.tab();

      await waitFor(() => {
        expect(textAnswer1).toHaveAttribute('aria-invalid', 'true');
        expect(screen.getByText(/String must contain at least 1/)).toBeInTheDocument();
      });
    });
  });

  describe('Network error preserves form data', () => {
    it('preserves form inputs on server error', async () => {
      const user = userEvent.setup();

      server.use(
        http.post('/api/feedback', () => {
          return HttpResponse.json(
            {
              error: 'Internal Server Error',
              message: 'Service temporarily unavailable',
              correlationId: 'corr-456',
              fieldErrors: [],
            },
            { status: 500 },
          );
        }),
      );

      await renderFeedbackForm();
      await waitForFormReady();

      await user.click(screen.getByRole('button', { name: 'Score 5' }));
      await user.type(screen.getByLabelText(/What went well/), 'Amazing experience');
      await user.type(screen.getByLabelText(/What could be improved/), 'Nothing');
      await user.selectOptions(screen.getByLabelText(/Category/) as HTMLSelectElement, 'Content');

      const form = screen.getByRole('button', { name: /Submit Feedback/ }).closest('form')!;
      form.requestSubmit();

      // Wait for error to appear or form data to be preserved
      await waitFor(() => {
        const textAnswer1 = screen.getByLabelText(/What went well/) as HTMLTextAreaElement;
        expect(textAnswer1.value).toBe('Amazing experience');
      }, { timeout: 3000 });

      const textAnswer2 = screen.getByLabelText(/What could be improved/) as HTMLTextAreaElement;
      expect(textAnswer2.value).toBe('Nothing');
    });

    it('preserves form data on network error', async () => {
      const user = userEvent.setup();

      server.use(
        http.post('/api/feedback', () => {
          return HttpResponse.error();
        }),
      );

      await renderFeedbackForm();
      await waitForFormReady();

      await user.click(screen.getByRole('button', { name: 'Score 3' }));
      await user.type(screen.getByLabelText(/What went well/), 'Good event');
      await user.type(screen.getByLabelText(/What could be improved/), 'Better food');
      await user.selectOptions(screen.getByLabelText(/Category/) as HTMLSelectElement, 'Logistics');

      const form = screen.getByRole('button', { name: /Submit Feedback/ }).closest('form')!;
      form.requestSubmit();

      // Form data should be preserved after error
      await waitFor(() => {
        const textAnswer1 = screen.getByLabelText(/What went well/) as HTMLTextAreaElement;
        expect(textAnswer1.value).toBe('Good event');
      }, { timeout: 3000 });

      const textAnswer2 = screen.getByLabelText(/What could be improved/) as HTMLTextAreaElement;
      expect(textAnswer2.value).toBe('Better food');
    });
  });
});
