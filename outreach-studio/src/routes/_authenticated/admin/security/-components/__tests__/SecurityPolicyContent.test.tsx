import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import type { SecurityPolicy } from '@/types/domain';

const mockToastSuccess = vi.fn();
const mockToastError = vi.fn();
vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({
    toast: vi.fn(), success: mockToastSuccess, error: mockToastError, warning: vi.fn(), info: vi.fn(), dismiss: vi.fn(),
  }),
}));

const off = { enabled: false, length: 6, ttlSeconds: 300, maxAttempts: 5, resendCooldownSeconds: 30 };
const policy: SecurityPolicy = {
  passwordMinLength: 12,
  passwordHistoryCount: 5,
  platformMinLength: 12,
  platformHistoryCount: 5,
  otp: { LOGIN: off, PASSWORD_RESET: { ...off, ttlSeconds: 600 }, PASSWORD_CHANGE: off },
};

async function renderPage() {
  const { SecurityPolicyContent } = await import('../SecurityPolicyContent');
  renderWithProviders(<SecurityPolicyContent />);
  await screen.findByLabelText('Minimum length');
}

describe('SecurityPolicyContent', () => {
  beforeEach(() => {
    mockToastSuccess.mockClear();
    mockToastError.mockClear();
    server.use(http.get('/api/auth/security-policy', () => HttpResponse.json(policy)));
  });

  it('shows the current policy with code settings locked while codes are off', async () => {
    await renderPage();

    expect(screen.getByLabelText('Minimum length')).toHaveValue(12);
    expect(screen.getByText('Platform minimum: 12')).toBeInTheDocument();
    expect(screen.getByLabelText('Password reset: expires after minutes')).toHaveValue(10);
    expect(screen.getByLabelText('Sign-in: attempts')).toBeDisabled();
  });

  it('turns on sign-in codes and saves the whole policy', async () => {
    const user = userEvent.setup();
    let body: SecurityPolicy | null = null;
    server.use(http.put('/api/auth/security-policy', async ({ request }) => {
      body = (await request.json()) as SecurityPolicy;
      return HttpResponse.json({ ...policy, ...body });
    }));
    await renderPage();

    await user.click(screen.getByRole('checkbox', { name: /Sign-in/ }));
    await user.clear(screen.getByLabelText('Sign-in: attempts'));
    await user.type(screen.getByLabelText('Sign-in: attempts'), '3');
    await user.clear(screen.getByLabelText('Minimum length'));
    await user.type(screen.getByLabelText('Minimum length'), '14');
    await user.click(screen.getByRole('button', { name: 'Save policy' }));

    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalled());
    expect(body!.passwordMinLength).toBe(14);
    expect(body!.otp.LOGIN).toEqual({ ...off, enabled: true, maxAttempts: 3 });
  });

  it('shows the server refusing a looser rule', async () => {
    const user = userEvent.setup();
    server.use(http.put('/api/auth/security-policy', () => HttpResponse.json({
      status: 400, error: 'INVALID_REQUEST',
      message: 'Minimum password length cannot be below the platform minimum of 12', fieldErrors: {},
    }, { status: 400 })));
    await renderPage();

    await user.clear(screen.getByLabelText('Minimum length'));
    await user.type(screen.getByLabelText('Minimum length'), '8');
    await user.click(screen.getByRole('button', { name: 'Save policy' }));

    await waitFor(() => expect(mockToastError).toHaveBeenCalledWith(
      'Minimum password length cannot be below the platform minimum of 12'));
  });
});
