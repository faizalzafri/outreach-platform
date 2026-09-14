import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NoTenantPage } from '../no-tenant';

vi.mock('@/lib/auth', () => ({
  authModule: {
    login: vi.fn(),
  },
}));

import { authModule } from '@/lib/auth';

describe('NoTenantPage', () => {
  it('renders an explanatory message', () => {
    render(<NoTenantPage />);
    expect(screen.getByRole('heading', { name: /no tenant access/i })).toBeInTheDocument();
    expect(screen.getByText(/isn't associated with any organization/i)).toBeInTheDocument();
  });

  it('calls authModule.login() when "Try Again" is clicked', async () => {
    const user = userEvent.setup();
    render(<NoTenantPage />);

    await user.click(screen.getByRole('button', { name: /try again/i }));

    expect(authModule.login).toHaveBeenCalledOnce();
  });
});
