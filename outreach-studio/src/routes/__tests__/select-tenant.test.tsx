import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { SelectTenantPage } from '../select-tenant';
import { MOCK_TENANT_MEMBERSHIPS } from '@/test/handlers';

const navigateSpy = vi.fn();

vi.mock('@tanstack/react-router', async () => {
  const actual = await vi.importActual('@tanstack/react-router');
  return {
    ...actual,
    createFileRoute: () => () => ({ useSearch: () => ({}) }),
    useNavigate: () => navigateSpy,
  };
});

vi.mock('@/lib/auth', async () => {
  const actual = await vi.importActual<typeof import('@/lib/auth')>('@/lib/auth');
  return {
    ...actual,
    authModule: {
      ...actual.authModule,
      silentRefresh: vi.fn().mockResolvedValue('new-token'),
    },
  };
});

import { authModule } from '@/lib/auth';

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
    },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe('SelectTenantPage', () => {
  beforeEach(() => {
    localStorage.clear();
    navigateSpy.mockClear();
    vi.mocked(authModule.silentRefresh).mockClear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('lists all active tenant memberships', async () => {
    render(<SelectTenantPage />, { wrapper: createWrapper() });

    for (const membership of MOCK_TENANT_MEMBERSHIPS) {
      await waitFor(() => {
        expect(screen.getByText(membership.tenantName)).toBeInTheDocument();
      });
    }
  });

  it('selecting a tenant records the choice, refreshes the token, and navigates home', async () => {
    const user = userEvent.setup();
    render(<SelectTenantPage />, { wrapper: createWrapper() });

    const firstTenant = MOCK_TENANT_MEMBERSHIPS[0]!;
    const card = await screen.findByText(firstTenant.tenantName);
    await user.click(card);

    await waitFor(() => {
      expect(authModule.silentRefresh).toHaveBeenCalledOnce();
    });
    expect(localStorage.getItem('outreach-last-tenant')).toBe(firstTenant.tenantId);
    expect(navigateSpy).toHaveBeenCalledWith({ to: '/' });
  });

  it('shows the last-used label on the previously selected tenant', async () => {
    const lastUsed = MOCK_TENANT_MEMBERSHIPS[1]!;
    localStorage.setItem('outreach-last-tenant', lastUsed.tenantId);

    render(<SelectTenantPage />, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(screen.getByText('Last used')).toBeInTheDocument();
    });
  });

  it('shows an error state with retry when the membership list fails to load', async () => {
    server.use(
      http.get('/api/auth/tenant-memberships', () => {
        return HttpResponse.json({ error: 'SERVER_ERROR', message: 'Failed' }, { status: 500 });
      }),
    );

    render(<SelectTenantPage />, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(screen.getByText(/could not load your organizations/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });
});
