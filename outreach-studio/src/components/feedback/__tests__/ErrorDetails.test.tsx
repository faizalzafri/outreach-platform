import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { ErrorDetails } from '../ErrorDetails';
import type { NormalizedError } from '@/types/api';

function createError(overrides: Partial<NormalizedError> = {}): NormalizedError {
  return {
    status: 500,
    type: 'INTERNAL_SERVER_ERROR',
    message: 'Something went wrong on the server',
    correlationId: 'abc-123-def',
    fieldErrors: [],
    ...overrides,
  };
}

describe('ErrorDetails', () => {
  it('displays the error message', () => {
    const error = createError({ message: 'User-facing error message' });

    render(<ErrorDetails error={error} />);

    expect(screen.getByText('User-facing error message')).toBeInTheDocument();
  });

  it('hides technical details behind expandable section', () => {
    const error = createError({ status: 422, type: 'VALIDATION_ERROR' });

    render(<ErrorDetails error={error} />);

    // Details toggle should exist but content should be hidden
    const toggle = screen.getByRole('button', { name: /details/i });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');

    // Status code should not be visible initially
    expect(screen.queryByText(/Status: 422/)).not.toBeInTheDocument();
  });

  it('shows status, type, and correlation ID when details are expanded', async () => {
    const user = userEvent.setup();
    const error = createError({
      status: 403,
      type: 'FORBIDDEN',
      correlationId: 'corr-id-999',
    });

    render(<ErrorDetails error={error} />);

    await user.click(screen.getByRole('button', { name: /details/i }));

    const detailsContent = screen.getByText(/Status: 403/);
    expect(detailsContent).toBeInTheDocument();
    expect(detailsContent).toHaveTextContent('Type: FORBIDDEN');
    expect(detailsContent).toHaveTextContent('Correlation ID: corr-id-999');
  });

  it('hides correlation ID when null', async () => {
    const user = userEvent.setup();
    const error = createError({ correlationId: null });

    render(<ErrorDetails error={error} />);

    await user.click(screen.getByRole('button', { name: /details/i }));

    expect(screen.queryByText(/Correlation ID/)).not.toBeInTheDocument();
  });

  it('displays field errors when present', async () => {
    const user = userEvent.setup();
    const error = createError({
      fieldErrors: [
        { field: 'email', message: 'Invalid email format' },
        { field: 'name', message: 'Name is required' },
      ],
    });

    render(<ErrorDetails error={error} />);

    await user.click(screen.getByRole('button', { name: /details/i }));

    expect(screen.getByText(/Field Errors/)).toBeInTheDocument();
    expect(screen.getByText(/email: Invalid email format/)).toBeInTheDocument();
    expect(screen.getByText(/name: Name is required/)).toBeInTheDocument();
  });

  it('toggles details closed when clicked again', async () => {
    const user = userEvent.setup();
    const error = createError();

    render(<ErrorDetails error={error} />);

    const toggle = screen.getByRole('button', { name: /details/i });

    // Open
    await user.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText(/Status: 500/)).toBeInTheDocument();

    // Close
    await user.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByText(/Status: 500/)).not.toBeInTheDocument();
  });

  it('has role="alert" for accessibility', () => {
    const error = createError();

    render(<ErrorDetails error={error} />);

    expect(screen.getByRole('alert')).toBeInTheDocument();
  });
});
