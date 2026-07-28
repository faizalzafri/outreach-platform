import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { OfflineBanner } from '../OfflineBanner';
import { SystemUnavailableBanner } from '../SystemUnavailableBanner';

describe('OfflineBanner', () => {
  it('renders offline message', () => {
    render(<OfflineBanner />);

    expect(
      screen.getByText(/you are offline/i),
    ).toBeInTheDocument();
  });

  it('has role="alert" for screen reader announcement', () => {
    render(<OfflineBanner />);

    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveAttribute('aria-live', 'assertive');
    expect(alert).toHaveAttribute('aria-atomic', 'true');
  });

  it('informs user that changes will sync on reconnect', () => {
    render(<OfflineBanner />);

    expect(
      screen.getByText(/changes will be saved and synced when your connection is restored/i),
    ).toBeInTheDocument();
  });
});

describe('SystemUnavailableBanner', () => {
  it('renders system unavailable message', () => {
    render(<SystemUnavailableBanner />);

    expect(
      screen.getByText(/system unavailable/i),
    ).toBeInTheDocument();
  });

  it('has role="alert" with assertive live region', () => {
    render(<SystemUnavailableBanner />);

    const alert = screen.getByRole('alert');
    expect(alert).toHaveAttribute('aria-live', 'assertive');
  });

  it('instructs user to try again later', () => {
    render(<SystemUnavailableBanner />);

    expect(
      screen.getByText(/please try again later/i),
    ).toBeInTheDocument();
  });
});
