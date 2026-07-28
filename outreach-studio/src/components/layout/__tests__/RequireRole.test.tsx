import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { RequireRole } from '../RequireRole';

// Mock the usePermission hook to control test scenarios
vi.mock('@/hooks/usePermission', () => ({
  usePermission: vi.fn(),
}));

import { usePermission } from '@/hooks/usePermission';
const mockUsePermission = vi.mocked(usePermission);

describe('RequireRole', () => {
  it('renders children when user has at least one required role', () => {
    mockUsePermission.mockReturnValue({ hasPermission: true, isLoading: false });

    render(
      <RequireRole roles={['ROLE_ADMIN']}>
        <button data-testid="admin-action">Delete User</button>
      </RequireRole>
    );

    expect(screen.getByTestId('admin-action')).toBeInTheDocument();
  });

  it('renders nothing when user lacks required role', () => {
    mockUsePermission.mockReturnValue({ hasPermission: false, isLoading: false });

    const { container } = render(
      <RequireRole roles={['ROLE_ADMIN']}>
        <button data-testid="admin-action">Delete User</button>
      </RequireRole>
    );

    expect(screen.queryByTestId('admin-action')).not.toBeInTheDocument();
    expect(container.innerHTML).toBe('');
  });

  it('renders nothing while loading (avoids flicker)', () => {
    mockUsePermission.mockReturnValue({ hasPermission: false, isLoading: true });

    const { container } = render(
      <RequireRole roles={['ROLE_ADMIN']}>
        <button data-testid="admin-action">Delete User</button>
      </RequireRole>
    );

    expect(screen.queryByTestId('admin-action')).not.toBeInTheDocument();
    expect(container.innerHTML).toBe('');
  });

  it('passes roles array to usePermission', () => {
    mockUsePermission.mockReturnValue({ hasPermission: true, isLoading: false });

    render(
      <RequireRole roles={['ROLE_PMO', 'ROLE_ADMIN']}>
        <span>Conditional UI</span>
      </RequireRole>
    );

    expect(mockUsePermission).toHaveBeenCalledWith(['ROLE_PMO', 'ROLE_ADMIN']);
  });

  it('renders multiple children when authorized', () => {
    mockUsePermission.mockReturnValue({ hasPermission: true, isLoading: false });

    render(
      <RequireRole roles={['ROLE_ADMIN']}>
        <span data-testid="item-1">Item 1</span>
        <span data-testid="item-2">Item 2</span>
      </RequireRole>
    );

    expect(screen.getByTestId('item-1')).toBeInTheDocument();
    expect(screen.getByTestId('item-2')).toBeInTheDocument();
  });
});
