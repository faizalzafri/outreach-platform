/**
 * Header Component
 *
 * Displays the top header bar with:
 * - Hamburger toggle button for mobile sidebar
 * - Authenticated user's name and role
 * - Logout button
 *
 * Requirements: 2.1, 2.3
 */

import type { UserProfile } from '@/types/auth';
import styles from './Header.module.css';

export interface HeaderProps {
  user: UserProfile;
  onLogout: () => void;
  onToggleSidebar: () => void;
}

/**
 * Formats a role string for display (e.g., "ROLE_ADMIN" → "Admin")
 */
function formatRole(role: string): string {
  return role
    .replace(/^ROLE_/, '')
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
}

export function Header({ user, onLogout, onToggleSidebar }: HeaderProps) {
  const firstRole = user.roles[0];
  const primaryRole = firstRole ? formatRole(firstRole) : 'User';

  return (
    <header className={styles.header} role="banner">
      <div className={styles.headerLeft}>
        <button
          className={styles.hamburgerBtn}
          onClick={onToggleSidebar}
          aria-label="Toggle navigation menu"
          type="button"
        >
          <svg
            className={styles.hamburgerIcon}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M4 6h16M4 12h16M4 18h16"
            />
          </svg>
        </button>
      </div>

      <div className={styles.headerRight}>
        <div className={styles.userInfo}>
          <span className={styles.userName}>{user.name}</span>
          <span className={styles.userRole}>{primaryRole}</span>
        </div>

        <button
          className={styles.logoutBtn}
          onClick={onLogout}
          type="button"
          aria-label="Log out"
        >
          <svg
            className={styles.logoutIcon}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
            />
          </svg>
          Logout
        </button>
      </div>
    </header>
  );
}
