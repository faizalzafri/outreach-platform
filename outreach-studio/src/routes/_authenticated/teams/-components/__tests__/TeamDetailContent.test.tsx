import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import { MOCK_TEAMS, MOCK_TEAM_MEMBERS, MOCK_AVAILABLE_USERS } from '@/test/handlers';

const navigateSpy = vi.fn();

vi.mock('@tanstack/react-router', async () => {
  const actual = await vi.importActual('@tanstack/react-router');
  return {
    ...actual,
    useNavigate: () => navigateSpy,
  };
});

let mockSearch: { edit: boolean } = { edit: false };
vi.mock('../../$teamId', () => ({
  Route: {
    useParams: () => ({ teamId: 'team-001' }),
    useSearch: () => mockSearch,
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

describe('TeamDetailContent', () => {
  beforeEach(() => {
    mockSearch = { edit: false };
    navigateSpy.mockClear();
    mockToastSuccess.mockClear();
    mockToastError.mockClear();
  });

  async function renderTeamDetail() {
    const { TeamDetailContent } = await import('../TeamDetailContent');
    return renderWithProviders(<TeamDetailContent />);
  }

  it('displays team details and members', async () => {
    await renderTeamDetail();

    await waitFor(() => {
      expect(screen.getByText(MOCK_TEAMS[0]!.name)).toBeInTheDocument();
    });
    expect(screen.getByText(MOCK_TEAMS[0]!.description)).toBeInTheDocument();

    for (const member of MOCK_TEAM_MEMBERS) {
      expect(screen.getByText(member.username)).toBeInTheDocument();
    }
  });

  it('opens directly in edit mode when the edit search param is set', async () => {
    mockSearch = { edit: true };
    await renderTeamDetail();

    await waitFor(() => {
      expect(screen.getByLabelText('Name')).toBeInTheDocument();
    });
    expect(screen.getByDisplayValue(MOCK_TEAMS[0]!.name)).toBeInTheDocument();
  });

  it('saves an edit and shows a success toast', async () => {
    const user = userEvent.setup();
    await renderTeamDetail();

    await waitFor(() => {
      expect(screen.getByText(MOCK_TEAMS[0]!.name)).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Edit' }));
    const nameInput = await screen.findByLabelText('Name');
    await user.clear(nameInput);
    await user.type(nameInput, 'Renamed Team');

    const submitBtn = screen.getByRole('button', { name: /save changes/i });
    await waitFor(() => expect(submitBtn).not.toBeDisabled());
    await user.click(submitBtn);

    await waitFor(() => {
      expect(mockToastSuccess).toHaveBeenCalledWith('Team updated successfully');
    });
  });

  it('searches for and adds an available user as a member', async () => {
    const user = userEvent.setup();
    await renderTeamDetail();

    await waitFor(() => {
      expect(screen.getByText(MOCK_TEAMS[0]!.name)).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /add member/i }));
    const searchInput = screen.getByLabelText('Search available users');
    await user.type(searchInput, MOCK_AVAILABLE_USERS[0]!.username.slice(0, 5));

    await waitFor(() => {
      expect(screen.getByText(MOCK_AVAILABLE_USERS[0]!.username)).toBeInTheDocument();
    });

    await user.click(screen.getByText(MOCK_AVAILABLE_USERS[0]!.username));

    await waitFor(() => {
      expect(mockToastSuccess).toHaveBeenCalledWith('Member added');
    });
  });

  it('removes a member and shows a success toast', async () => {
    const user = userEvent.setup();
    await renderTeamDetail();

    await waitFor(() => {
      expect(screen.getByText(MOCK_TEAM_MEMBERS[0]!.username)).toBeInTheDocument();
    });

    const removeBtn = screen.getByRole('button', {
      name: `Remove ${MOCK_TEAM_MEMBERS[0]!.username} from team`,
    });
    await user.click(removeBtn);

    await waitFor(() => {
      expect(mockToastSuccess).toHaveBeenCalledWith('Member removed');
    });
  });

  it('shows an error state with retry when the team fails to load', async () => {
    server.use(
      http.get('/api/teams/:id', () => {
        return HttpResponse.json({ error: 'NOT_FOUND', message: 'Team not found' }, { status: 404 });
      }),
    );

    await renderTeamDetail();

    await waitFor(() => {
      expect(screen.getByText(/failed to load team/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });
});
