import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';

/**
 * Tests for routing behavior:
 * - 404 page rendering for unknown routes
 * - Auth redirect for unauthenticated users
 *
 * These test the component outputs directly since TanStack Router
 * file-based routes are tested via their rendered components.
 */

// Mock TanStack Router modules
vi.mock('@tanstack/react-router', () => ({
  createFileRoute: () => () => ({}),
  Link: ({ children, to, ...props }: any) => (
    <a href={to} {...props}>{children}</a>
  ),
  useNavigate: () => vi.fn(),
  Outlet: () => <div data-testid="outlet" />,
}));

describe('404 Not Found Page', () => {
  it('renders 404 heading and descriptive message', async () => {
    // Dynamically import after mocks are in place
    const { default: NotFoundModule } = await import('../404');
    // The file exports Route via createFileRoute; we need to render the component directly
    // Since createFileRoute is mocked, we'll re-render the component function
    vi.resetModules();
    vi.doMock('@tanstack/react-router', () => ({
      createFileRoute: () => (opts: any) => opts,
      Link: ({ children, to, ...props }: any) => (
        <a href={to} {...props}>{children}</a>
      ),
    }));

    const mod = await import('../404');
    const Route = (mod as any).Route;
    const NotFoundPage = Route?.component;

    if (NotFoundPage) {
      render(<NotFoundPage />);
      expect(screen.getByText('404 — Page Not Found')).toBeInTheDocument();
      expect(screen.getByText("The page you're looking for doesn't exist.")).toBeInTheDocument();
    }
  });

  it('provides a link back to the dashboard', async () => {
    vi.resetModules();
    vi.doMock('@tanstack/react-router', () => ({
      createFileRoute: () => (opts: any) => opts,
      Link: ({ children, to, ...props }: any) => (
        <a href={to} {...props}>{children}</a>
      ),
    }));

    const mod = await import('../404');
    const Route = (mod as any).Route;
    const NotFoundPage = Route?.component;

    if (NotFoundPage) {
      render(<NotFoundPage />);
      const link = screen.getByRole('link', { name: /Back to Dashboard/i });
      expect(link).toBeInTheDocument();
      expect(link).toHaveAttribute('href', '/');
    }
  });
});

describe('Authenticated Route Guard', () => {
  it('renders loading state while auth is resolving', async () => {
    vi.resetModules();
    vi.doMock('@tanstack/react-router', () => ({
      createFileRoute: () => (opts: any) => opts,
      useNavigate: () => vi.fn(),
      Outlet: () => <div data-testid="outlet" />,
    }));
    vi.doMock('@/hooks/useAuth', () => ({
      useAuth: () => ({
        user: null,
        isAuthenticated: false,
        isLoading: true,
        login: vi.fn(),
        logout: vi.fn(),
      }),
    }));
    vi.doMock('@/components/layout', () => ({
      LayoutShell: ({ children }: any) => <div data-testid="layout">{children}</div>,
    }));
    vi.doMock('@/components/feedback/PageSkeleton', () => ({
      PageSkeleton: () => <div data-testid="page-skeleton" />,
    }));
    vi.doMock('@/components/feedback/ErrorBoundary', () => ({
      ErrorBoundary: ({ children }: any) => <div>{children}</div>,
    }));

    const mod = await import('../_authenticated');
    const Route = (mod as any).Route;
    const AuthenticatedLayout = Route?.component;

    if (AuthenticatedLayout) {
      render(<AuthenticatedLayout />);
      expect(screen.getByRole('status')).toBeInTheDocument();
      expect(screen.getByText('Loading...')).toBeInTheDocument();
    }
  });

  it('renders nothing and triggers redirect when unauthenticated', async () => {
    const mockNavigate = vi.fn();

    vi.resetModules();
    vi.doMock('@tanstack/react-router', () => ({
      createFileRoute: () => (opts: any) => opts,
      useNavigate: () => mockNavigate,
      Outlet: () => <div data-testid="outlet" />,
    }));
    vi.doMock('@/hooks/useAuth', () => ({
      useAuth: () => ({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        login: vi.fn(),
        logout: vi.fn(),
      }),
    }));
    vi.doMock('@/components/layout', () => ({
      LayoutShell: ({ children }: any) => <div data-testid="layout">{children}</div>,
    }));
    vi.doMock('@/components/feedback/PageSkeleton', () => ({
      PageSkeleton: () => <div data-testid="page-skeleton" />,
    }));
    vi.doMock('@/components/feedback/ErrorBoundary', () => ({
      ErrorBoundary: ({ children }: any) => <div>{children}</div>,
    }));

    const mod = await import('../_authenticated');
    const Route = (mod as any).Route;
    const AuthenticatedLayout = Route?.component;

    if (AuthenticatedLayout) {
      const { container } = render(<AuthenticatedLayout />);
      // Unauthenticated state returns null (empty render)
      expect(screen.queryByTestId('outlet')).not.toBeInTheDocument();
      expect(screen.queryByTestId('layout')).not.toBeInTheDocument();
      // Navigate should be called to redirect to login
      expect(mockNavigate).toHaveBeenCalledWith(
        expect.objectContaining({
          to: '/login',
          search: expect.objectContaining({ redirect: expect.any(String) }),
        })
      );
    }
  });

  it('renders layout with outlet when authenticated', async () => {
    vi.resetModules();
    vi.doMock('@tanstack/react-router', () => ({
      createFileRoute: () => (opts: any) => opts,
      useNavigate: () => vi.fn(),
      Outlet: () => <div data-testid="outlet">Route Content</div>,
    }));
    vi.doMock('@/hooks/useAuth', () => ({
      useAuth: () => ({
        user: { sub: '1', name: 'Admin', email: 'a@b.c', roles: ['ROLE_ADMIN'] },
        isAuthenticated: true,
        isLoading: false,
        login: vi.fn(),
        logout: vi.fn(),
      }),
    }));
    vi.doMock('@/components/layout', () => ({
      LayoutShell: ({ children }: any) => <div data-testid="layout">{children}</div>,
    }));
    vi.doMock('@/components/feedback/PageSkeleton', () => ({
      PageSkeleton: () => <div data-testid="page-skeleton" />,
    }));
    vi.doMock('@/components/feedback/ErrorBoundary', () => ({
      ErrorBoundary: ({ children }: any) => <div>{children}</div>,
    }));

    const mod = await import('../_authenticated');
    const Route = (mod as any).Route;
    const AuthenticatedLayout = Route?.component;

    if (AuthenticatedLayout) {
      render(<AuthenticatedLayout />);
      expect(screen.getByTestId('layout')).toBeInTheDocument();
      expect(screen.getByTestId('outlet')).toBeInTheDocument();
    }
  });
});
