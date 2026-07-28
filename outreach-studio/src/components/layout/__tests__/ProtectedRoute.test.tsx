import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ProtectedRoute } from '../ProtectedRoute';

// Mock the usePermission hook to control test scenarios
vi.mock('@/hooks/usePermission', () => ({
  usePermission: vi.fn(),
}));

import { usePermission } from '@/hooks/usePermission';
const mockUsePermission = vi.mocked(usePermission);

describe('ProtectedRoute', () => {
  it('renders children when user has required role', () => {
    mockUsePermission.mockReturnValue({ hasPermission: true, isLoading: false });

    render(
      <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
        <div data-testid="protected-content">Secret Content</div>
      </ProtectedRoute>
    );

    expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    expect(screen.getByText('Secret Content')).toBeInTheDocument();
  });

  it('renders 403 Forbidden page when user lacks required role', () => {
    mockUsePermission.mockReturnValue({ hasPermission: false, isLoading: false });

    render(
      <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
        <div data-testid="protected-content">Secret Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    expect(screen.getByText('Access Denied')).toBeInTheDocument();
    expect(screen.getByText(/You do not have permission/)).toBeInTheDocument();
  });

  it('renders loading indicator while permissions are resolving', () => {
    mockUsePermission.mockReturnValue({ hasPermission: false, isLoading: true });

    render(
      <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
        <div data-testid="protected-content">Secret Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText('Verifying access...')).toBeInTheDocument();
  });

  it('passes requiredRoles to usePermission hook', () => {
    mockUsePermission.mockReturnValue({ hasPermission: true, isLoading: false });

    render(
      <ProtectedRoute requiredRoles={['ROLE_ADMIN', 'ROLE_PMO']}>
        <div>Content</div>
      </ProtectedRoute>
    );

    expect(mockUsePermission).toHaveBeenCalledWith(['ROLE_ADMIN', 'ROLE_PMO']);
  });

  it('renders 403 page with link back to dashboard', () => {
    mockUsePermission.mockReturnValue({ hasPermission: false, isLoading: false });

    render(
      <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
        <div>Content</div>
      </ProtectedRoute>
    );

    const link = screen.getByRole('link', { name: /Return to Dashboard/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', '/dashboard');
  });
});
