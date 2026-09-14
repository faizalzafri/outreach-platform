import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import { MOCK_TEAMS } from '@/test/handlers';

const navigateSpy = vi.fn();

vi.mock('@tanstack/react-router', async () => {
  const actual = await vi.importActual('@tanstack/react-router');
  return {
    ...actual,
    useNavigate: () => navigateSpy,
  };
});

vi.mock('../../index', () => ({
  Route: {
    useSearch: () => ({ page: 1, size: 20 }),
  },
}));

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

describe('TeamListContent', () => {
  beforeEach(() => {
    navigateSpy.mockClear();
    mockToastSuccess.mockClear();
    mockToastError.mockClear();
  });

  async function renderTeamList() {
    const { TeamListContent } = await import('../TeamListContent');
    return renderWithProviders(<TeamListContent />);
  }

  it('lists teams from the API', async () => {
    await renderTeamList();

    for (const team of MOCK_TEAMS) {
      await waitFor(() => {
        expect(screen.getByText(team.name)).toBeInTheDocument();
      });
    }
  });

  it('shows the create form when Create Team is clicked', async () => {
    const user = userEvent.setup();
    await renderTeamList();

    await waitFor(() => {
      expect(screen.getByText(MOCK_TEAMS[0]!.name)).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /create team/i }));

    expect(screen.getByLabelText('Name')).toBeInTheDocument();
    expect(screen.getByLabelText('Description')).toBeInTheDocument();
  });

  it('disables submit until the name is at least 2 characters', async () => {
    const user = userEvent.setup();
    await renderTeamList();

    await waitFor(() => {
      expect(screen.getByText(MOCK_TEAMS[0]!.name)).toBeInTheDocument();
    });
    await user.click(screen.getByRole('button', { name: /create team/i }));

    const submitBtn = screen.getByRole('button', { name: /create team$/i, hidden: true });
    const nameInput = screen.getByLabelText('Name');

    await user.type(nameInput, 'A');
    expect(submitBtn).toBeDisabled();

    await user.type(nameInput, 'B');
    await waitFor(() => {
      expect(submitBtn).not.toBeDisabled();
    });
  });

  it('shows a duplicate-name field error from the server', async () => {
    server.use(
      http.post('/api/teams', () => {
        return HttpResponse.json(
          {
            error: 'Conflict',
            message: 'A team with this name already exists',
            fieldErrors: [{ field: 'name', message: 'Team name already in use' }],
          },
          { status: 409 },
        );
      }),
    );

    const user = userEvent.setup();
    await renderTeamList();

    await waitFor(() => {
      expect(screen.getByText(MOCK_TEAMS[0]!.name)).toBeInTheDocument();
    });
    await user.click(screen.getByRole('button', { name: /create team/i }));
    await user.type(screen.getByLabelText('Name'), 'Duplicate Team');

    const submitBtn = screen.getByRole('button', { name: /create team$/i, hidden: true });
    await waitFor(() => expect(submitBtn).not.toBeDisabled());
    await user.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Team name already in use')).toBeInTheDocument();
    });
  });

  it('shows a success toast and closes the form on successful creation', async () => {
    const user = userEvent.setup();
    await renderTeamList();

    await waitFor(() => {
      expect(screen.getByText(MOCK_TEAMS[0]!.name)).toBeInTheDocument();
    });
    await user.click(screen.getByRole('button', { name: /create team/i }));
    await user.type(screen.getByLabelText('Name'), 'New Team');

    const submitBtn = screen.getByRole('button', { name: /create team$/i, hidden: true });
    await waitFor(() => expect(submitBtn).not.toBeDisabled());
    await user.click(submitBtn);

    await waitFor(() => {
      expect(mockToastSuccess).toHaveBeenCalledWith('Team created successfully');
    });
    expect(screen.queryByLabelText('Name')).not.toBeInTheDocument();
  });

  it('shows a delete confirmation dialog and navigates to the team on View', async () => {
    const user = userEvent.setup();
    await renderTeamList();

    await waitFor(() => {
      expect(screen.getByText(MOCK_TEAMS[0]!.name)).toBeInTheDocument();
    });

    const viewButtons = screen.getAllByRole('button', { name: 'View' });
    await user.click(viewButtons[0]!);
    expect(navigateSpy).toHaveBeenCalledWith({
      to: '/teams/$teamId',
      params: { teamId: MOCK_TEAMS[0]!.id },
      search: { edit: false },
    });

    const deleteButtons = screen.getAllByRole('button', { name: 'Delete' });
    await user.click(deleteButtons[0]!);

    await waitFor(() => {
      expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });
    expect(screen.getByText(/revoke any resource permissions/i)).toBeInTheDocument();
  });
});
