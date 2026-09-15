import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { TenantBadge } from '../TenantBadge';
import { useTenantStore } from '@/stores/tenant-store';
import { useUIStore } from '@/stores/ui-store';
import { MOCK_TENANT } from '@/test/handlers';

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

describe('TenantBadge', () => {
  beforeEach(() => {
    useTenantStore.getState().setTenantFromJwt({ tenant_id: MOCK_TENANT.id, platform_admin: false });
  });

  afterEach(() => {
    useTenantStore.getState().clearTenant();
    useUIStore.setState({ toasts: [] });
  });

  it('renders the tenant name once loaded', async () => {
    render(<TenantBadge />, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(screen.getByText(MOCK_TENANT.name)).toBeInTheDocument();
    });
  });

  it('does not show the tenant name synchronously before the fetch resolves', () => {
    render(<TenantBadge />, { wrapper: createWrapper() });

    // Query hasn't resolved yet on this synchronous first render — only the loading placeholder
    // (no accessible "status" role) should be present.
    expect(screen.queryByText(MOCK_TENANT.name)).not.toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it(
    'falls back to a truncated tenant ID and warns when the fetch fails',
    async () => {
      server.use(
        http.get('/api/tenants/current', () => {
          return HttpResponse.json({ error: 'TENANT_NOT_FOUND', message: 'Not found' }, { status: 404 });
        }),
      );

      render(<TenantBadge />, { wrapper: createWrapper() });

      // useTenantName retries 3 times with exponential backoff before surfacing the error, so
      // this needs a longer timeout than the default.
      await waitFor(
        () => {
          expect(screen.getByText(MOCK_TENANT.id.slice(0, 8))).toBeInTheDocument();
        },
        { timeout: 10_000 },
      );

      await waitFor(() => {
        expect(useUIStore.getState().toasts).toHaveLength(1);
      });
      expect(useUIStore.getState().toasts[0]?.severity).toBe('warning');
      expect(useUIStore.getState().toasts[0]?.durationMs).toBe(8_000);
    },
    15_000,
  );
});
