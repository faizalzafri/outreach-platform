import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ErrorBoundary } from '../ErrorBoundary';

// Component that throws during render
function ThrowingComponent({ error }: { error: Error }) {
  throw error;
}

// Component that renders normally
function GoodComponent() {
  return <div>All good</div>;
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    // Suppress React's error boundary console.error output during tests
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  it('renders children when no error occurs', () => {
    render(
      <ErrorBoundary>
        <GoodComponent />
      </ErrorBoundary>,
    );

    expect(screen.getByText('All good')).toBeInTheDocument();
  });

  it('catches render errors and shows default fallback UI', () => {
    const testError = new Error('Test render failure');

    render(
      <ErrorBoundary>
        <ThrowingComponent error={testError} />
      </ErrorBoundary>,
    );

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('shows custom fallback when provided', () => {
    const testError = new Error('Crash');

    render(
      <ErrorBoundary fallback={<div>Custom fallback content</div>}>
        <ThrowingComponent error={testError} />
      </ErrorBoundary>,
    );

    expect(screen.getByText('Custom fallback content')).toBeInTheDocument();
    expect(screen.queryByText('Something went wrong')).not.toBeInTheDocument();
  });

  it('resets error state and calls onReset when retry is clicked', async () => {
    const user = userEvent.setup();
    const onReset = vi.fn();
    let shouldThrow = true;

    function ConditionalThrow() {
      if (shouldThrow) {
        throw new Error('Temporary failure');
      }
      return <div>Recovered</div>;
    }

    const { rerender } = render(
      <ErrorBoundary onReset={onReset}>
        <ConditionalThrow />
      </ErrorBoundary>,
    );

    // Should show fallback
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();

    // Fix the "error" and retry
    shouldThrow = false;

    await user.click(screen.getByRole('button', { name: /retry/i }));

    // Re-render so React picks up the new state
    rerender(
      <ErrorBoundary onReset={onReset}>
        <ConditionalThrow />
      </ErrorBoundary>,
    );

    expect(onReset).toHaveBeenCalledOnce();
  });

  it('shows error details behind expandable toggle', async () => {
    const user = userEvent.setup();
    const testError = new Error('Detailed error info');

    render(
      <ErrorBoundary>
        <ThrowingComponent error={testError} />
      </ErrorBoundary>,
    );

    // Details should be hidden initially
    const detailsButton = screen.getByRole('button', { name: /details/i });
    expect(detailsButton).toHaveAttribute('aria-expanded', 'false');

    // Click to show details
    await user.click(detailsButton);

    expect(detailsButton).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('Detailed error info')).toBeInTheDocument();
  });

  it('logs error with correlation ID in development mode', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const testError = new Error('Dev error');

    render(
      <ErrorBoundary>
        <ThrowingComponent error={testError} />
      </ErrorBoundary>,
    );

    // In test/dev mode, componentDidCatch should log with correlation ID
    const correlationCall = consoleSpy.mock.calls.find(
      (call) => typeof call[0] === 'string' && call[0].includes('[ErrorBoundary] Correlation ID:'),
    );
    expect(correlationCall).toBeDefined();

    consoleSpy.mockRestore();
  });
});
