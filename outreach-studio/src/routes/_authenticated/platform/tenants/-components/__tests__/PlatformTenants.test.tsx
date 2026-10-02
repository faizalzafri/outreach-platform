import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import type { TenantDetail, TenantSummary } from '@/types/tenant';

vi.mock('@tanstack/react-router', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@tanstack/react-router')>()),
  Link: ({ children, ...props }: { children: React.ReactNode }) => <a {...(props as object)}>{children}</a>,
}));

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({ user: { sub: 'platform_admin', roles: ['ROLE_PLATFORM_ADMIN'] }, isAuthenticated: true }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

const mockToastSuccess = vi.fn();
const mockToastError = vi.fn();
vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({
    toast: vi.fn(), success: mockToastSuccess, error: mockToastError, warning: vi.fn(), info: vi.fn(), dismiss: vi.fn(),
  }),
}));

const acme: TenantSummary = {
  id: 't-acme', name: 'Acme Volunteers', slug: 'acme', status: 'ACTIVE', createdDate: '2026-10-01T10:00:00Z', memberCount: 4,
};

describe('slugify', () => {
  it('turns a name into a valid short name', async () => {
    const { slugify } = await import('../TenantListContent');
    expect(slugify('  Acme Volunteers (India) ')).toBe('acme-volunteers-india');
    expect(slugify('Ünïcode Café')).toBe('unicode-cafe');
  });
});

describe('TenantListContent', () => {
  beforeEach(() => {
    mockToastSuccess.mockClear();
    mockToastError.mockClear();
    server.use(http.get('/api/tenants', () => HttpResponse.json({
      content: [acme], totalElements: 1, totalPages: 1, number: 0, size: 20,
    })));
  });

  it('lists organizations with status and member count', async () => {
    const { TenantListContent } = await import('../TenantListContent');
    renderWithProviders(<TenantListContent />);

    expect(await screen.findByText('Acme Volunteers')).toBeInTheDocument();
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
    expect(screen.getByText('4')).toBeInTheDocument();
  });

  it('creates an organization with its first admin, suggesting the short name', async () => {
    const user = userEvent.setup();
    let body: Record<string, unknown> | null = null;
    server.use(http.post('/api/tenants', async ({ request }) => {
      body = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json({}, { status: 201 });
    }));
    const { TenantListContent } = await import('../TenantListContent');
    renderWithProviders(<TenantListContent />);

    await user.click(await screen.findByRole('button', { name: 'New organization' }));
    await user.click(screen.getByRole('button', { name: 'Create organization' }));
    expect(await screen.findByText("Enter the admin's name")).toBeInTheDocument();
    expect(body).toBeNull();

    await user.type(screen.getByLabelText('Name'), 'Green Earth Trust');
    expect(screen.getByLabelText('Short name')).toHaveValue('green-earth-trust');
    await user.type(screen.getByLabelText('Full name'), 'Asha Menon');
    await user.type(screen.getByLabelText('Username'), 'asha');
    await user.type(screen.getByLabelText('Email'), 'asha@greenearth.example');
    await user.click(screen.getByRole('button', { name: 'Create organization' }));

    await waitFor(() => expect(body).toEqual({
      name: 'Green Earth Trust', slug: 'green-earth-trust',
      adminDisplayName: 'Asha Menon', adminUsername: 'asha', adminEmail: 'asha@greenearth.example',
    }));
    expect(mockToastSuccess).toHaveBeenCalledWith('Green Earth Trust created. Asha Menon has been emailed an invitation.');
  });
});

describe('TenantDetailContent', () => {
  const detail: TenantDetail = {
    ...acme, lastModifiedDate: null, lastModifiedBy: null, membersByRole: { ADMIN: 1, POC: 3 },
  };

  beforeEach(() => {
    server.use(
      http.get('/api/tenants/t-acme', () => HttpResponse.json(detail)),
      http.get('/api/auth/users', () => HttpResponse.json([])),
      http.get('/api/auth/security-policy', () => HttpResponse.json({
        passwordMinLength: 12, passwordHistoryCount: 5,
        otp: {
          LOGIN: { enabled: false, length: 6, ttlSeconds: 300, maxAttempts: 5, resendCooldownSeconds: 30 },
          PASSWORD_RESET: { enabled: false, length: 6, ttlSeconds: 300, maxAttempts: 5, resendCooldownSeconds: 30 },
          PASSWORD_CHANGE: { enabled: false, length: 6, ttlSeconds: 300, maxAttempts: 5, resendCooldownSeconds: 30 },
        },
      })),
    );
  });

  it('suspends only after confirmation, and manages users of that organization', async () => {
    const user = userEvent.setup();
    const suspended = vi.fn();
    let status: TenantDetail['status'] = 'ACTIVE';
    let usersTenant: string | null = null;
    server.use(
      http.get('/api/tenants/t-acme', () => HttpResponse.json({ ...detail, status })),
      http.post('/api/tenants/t-acme/suspend', () => {
        suspended();
        status = 'SUSPENDED';
        return HttpResponse.json({ ...detail, status: 'SUSPENDED' });
      }),
      http.get('/api/auth/users', ({ request }) => {
        usersTenant = new URL(request.url).searchParams.get('tenantId');
        return HttpResponse.json([]);
      }),
    );
    const confirm = vi.spyOn(window, 'confirm');
    const { TenantDetailContent } = await import('../TenantDetailContent');
    renderWithProviders(<TenantDetailContent tenantId="t-acme" />);

    expect(await screen.findByRole('heading', { name: 'Acme Volunteers' })).toBeInTheDocument();
    await waitFor(() => expect(usersTenant).toBe('t-acme'));

    confirm.mockReturnValueOnce(false);
    await user.click(screen.getByRole('button', { name: 'Suspend' }));
    expect(suspended).not.toHaveBeenCalled();

    confirm.mockReturnValueOnce(true);
    await user.click(screen.getByRole('button', { name: 'Suspend' }));
    expect(await screen.findByText('SUSPENDED')).toBeInTheDocument();
    expect(suspended).toHaveBeenCalledOnce();
  });
});
