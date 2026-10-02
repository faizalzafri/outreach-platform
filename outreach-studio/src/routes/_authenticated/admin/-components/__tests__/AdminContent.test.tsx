import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import type { Account } from '@/types/domain';

const mockUseAuth = vi.fn();
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
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

function account(overrides: Partial<Account>): Account {
  return {
    id: 'u-0',
    username: 'user',
    displayName: 'User',
    email: 'user@example.com',
    phone: null,
    role: 'POC',
    status: 'ACTIVE',
    locked: false,
    lastLoginAt: null,
    createdAt: '2026-10-01T10:00:00Z',
    ...overrides,
  };
}

const accounts: Account[] = [
  account({ id: 'u-admin', username: 'admin', displayName: 'Ada Admin', email: 'admin@example.com', role: 'ADMIN' }),
  account({ id: 'u-priya', username: 'priya_sharma', displayName: 'Priya Sharma', email: 'priya@example.com', role: 'PMO' }),
  account({ id: 'u-meera', username: 'meera', displayName: 'Meera Iyer', email: 'meera@example.com', status: 'INVITED' }),
  account({ id: 'u-rahul', username: 'rahul', displayName: 'Rahul Verma', email: 'rahul@example.com', locked: true }),
  account({ id: 'u-old', username: 'old', displayName: 'Old Timer', email: 'old@example.com', status: 'DISABLED' }),
];

async function renderAdmin() {
  const { AdminContent } = await import('../AdminContent');
  renderWithProviders(<AdminContent />);
  await screen.findByText('Priya Sharma');
}

function row(name: string) {
  return screen.getByText(name).closest('tr') as HTMLElement;
}

describe('AdminContent (user administration)', () => {
  beforeEach(() => {
    mockToastSuccess.mockClear();
    mockToastError.mockClear();
    mockUseAuth.mockReturnValue({
      user: { sub: 'admin', name: 'Admin', email: 'admin@example.com', roles: ['ROLE_ADMIN'] },
      isAuthenticated: true,
      isLoading: false,
    });
    server.use(http.get('/api/auth/users', () => HttpResponse.json(accounts)));
  });

  it('lists users with their status, including invited and locked accounts', async () => {
    await renderAdmin();

    expect(within(row('Meera Iyer')).getByText('INVITED')).toBeInTheDocument();
    expect(within(row('Rahul Verma')).getByText('Locked')).toBeInTheDocument();
    expect(within(row('Old Timer')).getByText('DISABLED')).toBeInTheDocument();
  });

  it('offers the actions that fit each status', async () => {
    await renderAdmin();

    const invited = within(row('Meera Iyer'));
    expect(invited.getByRole('button', { name: 'Resend invite' })).toBeInTheDocument();
    expect(invited.getByRole('button', { name: 'Revoke' })).toBeInTheDocument();
    expect(invited.queryByRole('button', { name: 'Send reset link' })).not.toBeInTheDocument();

    expect(within(row('Rahul Verma')).getByRole('button', { name: 'Unlock' })).toBeInTheDocument();
    expect(within(row('Old Timer')).getByRole('button', { name: 'Enable' })).toBeInTheDocument();
  });

  it('does not let an admin disable themselves or change their own role', async () => {
    await renderAdmin();

    const self = within(row('Ada Admin'));
    expect(self.queryByRole('button', { name: 'Disable' })).not.toBeInTheDocument();
    expect(self.getByRole('combobox', { name: 'Role for Ada Admin' })).toBeDisabled();
  });

  it('invites a user without any password and shows validation messages first', async () => {
    const user = userEvent.setup();
    let body: Record<string, unknown> | null = null;
    server.use(http.post('/api/auth/users', async ({ request }) => {
      body = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json(account({ username: 'kavya', email: 'kavya@example.com', status: 'INVITED' }), { status: 201 });
    }));
    await renderAdmin();

    await user.click(screen.getByRole('button', { name: 'Invite user' }));
    expect(screen.queryByLabelText(/password/i)).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Send invitation' }));
    expect(await screen.findByText("Enter the person's name")).toBeInTheDocument();
    expect(body).toBeNull();

    await user.type(screen.getByLabelText('Full name'), 'Kavya Rao');
    await user.type(screen.getByLabelText('Username'), 'kavya');
    await user.type(screen.getByLabelText('Email'), 'kavya@example.com');
    await user.selectOptions(screen.getByLabelText('Role'), 'PMO');
    await user.click(screen.getByRole('button', { name: 'Send invitation' }));

    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalledWith('Invitation sent to kavya@example.com'));
    expect(body).toEqual({ displayName: 'Kavya Rao', username: 'kavya', email: 'kavya@example.com', role: 'PMO' });
  });

  it('shows a server conflict as a message', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/auth/users', () => HttpResponse.json(
      { status: 409, error: 'USER_CONFLICT', message: "Username 'kavya' is already taken", fieldErrors: {} },
      { status: 409 })));
    await renderAdmin();

    await user.click(screen.getByRole('button', { name: 'Invite user' }));
    await user.type(screen.getByLabelText('Full name'), 'Kavya Rao');
    await user.type(screen.getByLabelText('Username'), 'kavya');
    await user.type(screen.getByLabelText('Email'), 'kavya@example.com');
    await user.click(screen.getByRole('button', { name: 'Send invitation' }));

    await waitFor(() => expect(mockToastError).toHaveBeenCalledWith("Username 'kavya' is already taken"));
  });

  it('revokes an invitation only after confirmation', async () => {
    const user = userEvent.setup();
    const revoked = vi.fn();
    server.use(http.delete('/api/auth/users/u-meera/invitation', () => {
      revoked();
      return new HttpResponse(null, { status: 204 });
    }));
    await renderAdmin();

    await user.click(within(row('Meera Iyer')).getByRole('button', { name: 'Revoke' }));
    expect(revoked).not.toHaveBeenCalled();
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Revoke' }));

    await waitFor(() => expect(revoked).toHaveBeenCalledOnce());
    expect(mockToastSuccess).toHaveBeenCalledWith('Invitation revoked');
  });

  it('changes a role through the auth API', async () => {
    const user = userEvent.setup();
    let roleBody: unknown = null;
    server.use(http.put('/api/auth/users/u-priya/role', async ({ request }) => {
      roleBody = await request.json();
      return HttpResponse.json(account({ id: 'u-priya', role: 'ADMIN' }));
    }));
    await renderAdmin();

    await user.selectOptions(screen.getByRole('combobox', { name: 'Role for Priya Sharma' }), 'ADMIN');

    await waitFor(() => expect(roleBody).toEqual({ role: 'ADMIN' }));
  });
});
