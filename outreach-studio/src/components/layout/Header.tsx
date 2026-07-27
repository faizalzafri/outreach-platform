/**
 * Header Component
 *
 * Displays the top header bar with:
 * - Search bar (cosmetic, no functionality yet)
 * - Theme toggle button (light/dark/system)
 * - "+ Create Event" quick-action button
 */

import { useUIStore } from '@/stores/ui-store';
import { useEffect } from 'react';
import styles from './Header.module.css';

export interface HeaderProps {
  onCreateEvent?: () => void;
  onToggleSidebar?: () => void;
}

export function Header({ onCreateEvent, onToggleSidebar }: HeaderProps) {
  const theme = useUIStore((s) => s.theme);
  const setTheme = useUIStore((s) => s.setTheme);

  // Apply data-theme attribute to <html> for CSS variable switching
  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'system') {
      root.removeAttribute('data-theme');
    } else {
      root.setAttribute('data-theme', theme);
    }
  }, [theme]);

  const cycleTheme = () => {
    const order: Array<'light' | 'dark' | 'system'> = ['light', 'dark', 'system'];
    const currentIndex = order.indexOf(theme);
    const next = order[(currentIndex + 1) % order.length]!;
    setTheme(next);
  };

  const themeIcon = theme === 'light' ? '☀️' : theme === 'dark' ? '🌙' : '🖥️';
  const themeLabel = theme === 'light' ? 'Light' : theme === 'dark' ? 'Dark' : 'System';

  return (
    <header className={styles.header} role="banner">
      <div className={styles.headerLeft}>
        {/* Mobile hamburger — visible only on small screens */}
        {onToggleSidebar && (
          <button
            className={styles.mobileMenuBtn}
            onClick={onToggleSidebar}
            type="button"
            aria-label="Open menu"
          >
            <svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        )}

        {/* Search bar (cosmetic) */}
        <div className={styles.searchBar}>
          <svg
            className={styles.searchIcon}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
            aria-hidden="true"
          >
            <circle cx="11" cy="11" r="8" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35" />
          </svg>
          <input
            type="text"
            className={styles.searchInput}
            placeholder="Search..."
            aria-label="Search"
            readOnly
          />
        </div>
      </div>

      <div className={styles.headerRight}>
        <button
          className={styles.themeBtn}
          onClick={cycleTheme}
          type="button"
          aria-label={`Theme: ${themeLabel}. Click to change.`}
          title={`Theme: ${themeLabel}`}
        >
          <span aria-hidden="true">{themeIcon}</span>
        </button>

        <button
          className={styles.createBtn}
          onClick={onCreateEvent}
          type="button"
          aria-label="Create Event"
        >
          + Create Event
        </button>
      </div>
    </header>
  );
}
