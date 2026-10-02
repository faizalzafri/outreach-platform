import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import type { Profile } from '@/types/domain';

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: { sub: 'meera', name: 'Meera Iyer', email: 'meera@example.com', roles: ['ROLE_POC'] },
    isAuthenticated: true,
    isLoading: false,
  }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

const mockToastSuccess = vi.fn();
const mockToastError = vi.fn();
vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({
    toast: vi.fn(), success: mockToastSuccess, error: mockToastError, warning: vi.fn(), info: vi.fn(), dismiss: vi.fn(),
  }),
}));

const profile: Profile = {
  id: 'u-1',
  username: 'meera',
  displayName: 'Meera Iyer',
  email: 'meera@example.com',
  phone: null,
  platformAdmin: false,
  lastLoginAt: '2026-10-01T09:00:00Z',
  passwordChangedAt: '2026-09-01T09:00:00Z',
  passwordMinLength: 12,
};

async function renderProfile() {
  const { ProfileContent } = await import('../ProfileContent');
  renderWithProviders(<ProfileContent />);
  await screen.findByText('meera@example.com');
}

describe('ProfileContent', () => {
  beforeEach(() => {
    mockToastSuccess.mockClear();
    mockToastError.mockClear();
    server.use(http.get('/api/auth/me', () => HttpResponse.json(profile)));
  });

  it('shows the account and its role', async () => {
    await renderProfile();

    expect(screen.getByText('meera')).toBeInTheDocument();
    expect(screen.getByText('POC')).toBeInTheDocument();
    expect(screen.getByText(/At least 12 characters/)).toBeInTheDocument();
  });

  it('saves name and phone', async () => {
    const user = userEvent.setup();
    let body: unknown = null;
    server.use(http.put('/api/auth/me', async ({ request }) => {
      body = await request.json();
      return HttpResponse.json({ ...profile, displayName: 'Meera I.', phone: '+91 98765 43210' });
    }));
    await renderProfile();

    await user.clear(screen.getByLabelText('Display name'));
    await user.type(screen.getByLabelText('Display name'), 'Meera I.');
    await user.type(screen.getByLabelText('Phone (optional)'), '+91 98765 43210');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => expect(body).toEqual({ displayName: 'Meera I.', phone: '+91 98765 43210' }));
    expect(mockToastSuccess).toHaveBeenCalled();
  });

  it('catches a mistyped confirmation before calling the server', async () => {
    const user = userEvent.setup();
    const called = vi.fn();
    server.use(http.post('/api/auth/me/password', () => {
      called();
      return new HttpResponse(null, { status: 204 });
    }));
    await renderProfile();

    await user.type(screen.getByLabelText('Current password'), 'Lotus-Garden-47');
    await user.type(screen.getByLabelText('New password'), 'Harbour-Lights-82');
    await user.type(screen.getByLabelText('Confirm new password'), 'Harbour-Lights-83');
    await user.click(screen.getByRole('button', { name: 'Change password' }));

    expect(await screen.findByText("The two passwords don't match")).toBeInTheDocument();
    expect(called).not.toHaveBeenCalled();
  });

  it('asks for the emailed code when the organization requires one, then sends it', async () => {
    const user = userEvent.setup();
    const bodies: Record<string, unknown>[] = [];
    server.use(http.post('/api/auth/me/password', async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>;
      bodies.push(body);
      if (!body['otpCode']) {
        return HttpResponse.json(
          { status: 400, error: 'OTP_REQUIRED', message: 'Enter the code we emailed you', fieldErrors: {} },
          { status: 400 });
      }
      return new HttpResponse(null, { status: 204 });
    }));
    await renderProfile();

    await user.type(screen.getByLabelText('Current password'), 'Lotus-Garden-47');
    await user.type(screen.getByLabelText('New password'), 'Harbour-Lights-82');
    await user.type(screen.getByLabelText('Confirm new password'), 'Harbour-Lights-82');
    await user.click(screen.getByRole('button', { name: 'Change password' }));

    await user.type(await screen.findByLabelText('Verification code'), '482913');
    await user.click(screen.getByRole('button', { name: 'Change password' }));

    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalledWith('Password changed. We emailed you a confirmation.'));
    expect(bodies).toHaveLength(2);
    expect(bodies[1]).toMatchObject({ otpCode: '482913', newPassword: 'Harbour-Lights-82' });
  });

  it('shows policy problems from the server under the new password', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/auth/me/password', () => HttpResponse.json({
      status: 400, error: 'PASSWORD_POLICY', message: 'The new password does not meet the password policy',
      fieldErrors: { newPassword: ['Password was used recently; choose one you have not used before'] },
    }, { status: 400 })));
    await renderProfile();

    await user.type(screen.getByLabelText('Current password'), 'Lotus-Garden-47');
    await user.type(screen.getByLabelText('New password'), 'Lotus-Garden-47');
    await user.type(screen.getByLabelText('Confirm new password'), 'Lotus-Garden-47');
    await user.click(screen.getByRole('button', { name: 'Change password' }));

    expect(await screen.findByText(/used recently/)).toBeInTheDocument();
  });
});
