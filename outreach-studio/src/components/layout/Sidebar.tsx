/**
 * Sidebar Navigation Component
 *
 * Renders a collapsible sidebar with navigation links grouped by domain.
 * Highlights the currently active route using TanStack Router's Link component.
 * Supports collapsed icon-only mode (64px) and expanded mode (260px).
 * Bottom section displays user profile, settings link, and logout action.
 */

import { useEffect, useRef, useCallback, useState } from 'react';
import { Link } from '@tanstack/react-router';
import type { UserProfile } from '@/types/auth';
import styles from './Sidebar.module.css';

export interface NavigationItem {
  label: string;
  href: string;
  icon: React.ComponentType;
  requiredRoles: string[];
}

export interface NavigationGroup {
  label: string;
  items: NavigationItem[];
}

export interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
  navigationGroups: NavigationGroup[];
  activeRoute: string;
  user: UserProfile;
  onLogout: () => void;
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

export function Sidebar({
  collapsed,
  onToggle,
  navigationGroups,
  activeRoute,
  user,
  onLogout,
}: SidebarProps) {
  const sidebarRef = useRef<HTMLElement>(null);
  const isMobile = useIsMobile();

  const firstRole = user.roles[0];
  const primaryRole = firstRole ? formatRole(firstRole) : 'User';
  const initials = user.name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  // Close sidebar on Escape key
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isMobile && !collapsed) {
        onToggle();
      }
    },
    [isMobile, collapsed, onToggle]
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  // Handle outside click on mobile overlay
  const handleOverlayClick = () => {
    if (isMobile && !collapsed) {
      onToggle();
    }
  };

  const isActive = (href: string): boolean => {
    if (href === '/dashboard') {
      return activeRoute === '/' || activeRoute === '/dashboard';
    }
    return activeRoute.startsWith(href);
  };

  const sidebarClasses = [
    styles.sidebar,
    collapsed ? styles.collapsed : '',
    isMobile && !collapsed ? styles.open : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <>
      {/* Mobile overlay backdrop */}
      {isMobile && !collapsed && (
        <div
          className={styles.overlay}
          onClick={handleOverlayClick}
          aria-hidden="true"
        />
      )}

      <nav
        ref={sidebarRef}
        className={sidebarClasses}
        aria-label="Main navigation"
        role="navigation"
      >
        {/* Brand area */}
        <div className={styles.brand}>
          <span
            className={styles.brandIcon}
            aria-hidden="true"
            onClick={collapsed ? onToggle : undefined}
            style={collapsed ? { cursor: 'pointer' } : undefined}
          >
            ◈
          </span>
          <span className={styles.brandText}>Outreach Studio</span>
          <button
            type="button"
            className={styles.collapseBtn}
            onClick={onToggle}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
        </div>

        {/* Navigation groups */}
        <div className={styles.navGroups}>
          {navigationGroups.map((group) => (
            <div key={group.label} className={styles.navGroup}>
              <div className={styles.groupLabel} aria-hidden="true">
                {group.label}
              </div>
              {group.items.map((item) => {
                const active = isActive(item.href);
                const Icon = item.icon;
                return (
                  <Link
                    key={item.href}
                    to={item.href}
                    className={`${styles.navItem} ${active ? styles.active : ''}`}
                    aria-current={active ? 'page' : undefined}
                    data-tooltip={collapsed ? item.label : undefined}
                    onClick={() => {
                      if (isMobile && !collapsed) {
                        onToggle();
                      }
                    }}
                  >
                    <span className={styles.navItemIcon} aria-hidden="true">
                      <Icon />
                    </span>
                    <span className={styles.navItemLabel}>{item.label}</span>
                  </Link>
                );
              })}
            </div>
          ))}
        </div>

        {/* Bottom section: user profile, settings, logout */}
        <div className={styles.bottomSection}>
          <div className={styles.userSection}>
            <div className={styles.avatar} aria-hidden="true">
              {initials}
            </div>
            <div className={styles.userDetails}>
              <span className={styles.userNameText}>{user.name}</span>
              <span className={styles.userRoleText}>{primaryRole}</span>
            </div>
          </div>

          <div className={styles.bottomActions}>
            <Link
              to="/admin"
              search={{ page: 1, size: 10 }}
              className={styles.bottomBtn}
              data-tooltip={collapsed ? 'Settings' : undefined}
              aria-label="Settings"
            >
              <span className={styles.bottomBtnIcon} aria-hidden="true">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="3" />
                  <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z" />
                </svg>
              </span>
              <span className={styles.bottomBtnLabel}>Settings</span>
            </Link>

            <button
              type="button"
              className={`${styles.bottomBtn} ${styles.danger}`}
              onClick={onLogout}
              data-tooltip={collapsed ? 'Log out' : undefined}
              aria-label="Log out"
            >
              <span className={styles.bottomBtnIcon} aria-hidden="true">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                  <polyline points="16 17 21 12 16 7" />
                  <line x1="21" y1="12" x2="9" y2="12" />
                </svg>
              </span>
              <span className={styles.bottomBtnLabel}>Log out</span>
            </button>
          </div>
        </div>
      </nav>
    </>
  );
}

/**
 * Custom hook to detect if the viewport is mobile-sized (< 768px).
 */
function useIsMobile(): boolean {
  const queryRef = useRef(
    typeof window !== 'undefined'
      ? window.matchMedia('(max-width: 767px)')
      : null
  );

  const getIsMobile = () => queryRef.current?.matches ?? false;

  const [isMobile, setIsMobile] = useState(getIsMobile);

  useEffect(() => {
    const mql = queryRef.current;
    if (!mql) return;

    const handler = (e: MediaQueryListEvent) => setIsMobile(e.matches);
    mql.addEventListener('change', handler);
    return () => mql.removeEventListener('change', handler);
  }, []);

  return isMobile;
}
