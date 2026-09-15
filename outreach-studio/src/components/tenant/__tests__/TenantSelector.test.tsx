import { describe, it, expect, afterEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TenantSelector } from '../TenantSelector';
import { useTenantStore } from '@/stores/tenant-store';
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

describe('TenantSelector', () => {
  afterEach(() => {
    useTenantStore.getState().setAdminSelectedTenant(null);
  });

  it('shows "All Tenants" as the trigger label by default', () => {
    render(<TenantSelector />, { wrapper: createWrapper() });
    expect(screen.getByRole('button', { name: /all tenants/i })).toBeInTheDocument();
  });

  it('opens the tenant list on trigger click', async () => {
    const user = userEvent.setup();
    render(<TenantSelector />, { wrapper: createWrapper() });

    await user.click(screen.getByRole('button', { name: /all tenants/i }));

    await waitFor(() => {
      expect(screen.getByText(MOCK_TENANT.name)).toBeInTheDocument();
    });
    expect(screen.getByRole('listbox', { name: /select tenant/i })).toBeInTheDocument();
  });

  it('selects a tenant and updates the store', async () => {
    const user = userEvent.setup();
    render(<TenantSelector />, { wrapper: createWrapper() });

    await user.click(screen.getByRole('button', { name: /all tenants/i }));

    const option = await screen.findByText(MOCK_TENANT.name);
    await user.click(option);

    expect(useTenantStore.getState().adminSelectedTenantId).toBe(MOCK_TENANT.id);
  });

  it('closes the panel after selecting a tenant', async () => {
    const user = userEvent.setup();
    render(<TenantSelector />, { wrapper: createWrapper() });

    await user.click(screen.getByRole('button', { name: /all tenants/i }));
    const option = await screen.findByText(MOCK_TENANT.name);
    await user.click(option);

    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('closes the panel on Escape', async () => {
    const user = userEvent.setup();
    render(<TenantSelector />, { wrapper: createWrapper() });

    await user.click(screen.getByRole('button', { name: /all tenants/i }));
    await waitFor(() => {
      expect(screen.getByRole('listbox')).toBeInTheDocument();
    });

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('always offers an "All Tenants" option to clear the override', async () => {
    const user = userEvent.setup();
    useTenantStore.getState().setAdminSelectedTenant(MOCK_TENANT.id);
    render(<TenantSelector />, { wrapper: createWrapper() });

    await user.click(screen.getByRole('button'));

    const listbox = await screen.findByRole('listbox');
    const clearOption = within(listbox).getByText('All Tenants');
    await user.click(clearOption);

    expect(useTenantStore.getState().adminSelectedTenantId).toBeNull();
  });
});
