/**
 * Sidebar Navigation Component
 *
 * Renders a collapsible sidebar with navigation links grouped by domain.
 * Highlights the currently active route. Supports responsive collapse
 * to overlay on mobile (< 768px) with outside click and Escape dismissal.
 *
 * Requirements: 2.1, 2.2, 2.4, 2.5
 */

import { useEffect, useRef, useCallback, useState } from 'react';
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
}

export function Sidebar({
  collapsed,
  onToggle,
  navigationGroups,
  activeRoute,
}: SidebarProps) {
  const sidebarRef = useRef<HTMLElement>(null);
  const isMobile = useIsMobile();

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
    if (href === '/') {
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
        <div className={styles.brand}>
          <span className={styles.brandIcon} aria-hidden="true">
            ◈
          </span>
          Outreach FMS
        </div>

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
                  <a
                    key={item.href}
                    href={item.href}
                    className={`${styles.navItem} ${active ? styles.active : ''}`}
                    aria-current={active ? 'page' : undefined}
                    onClick={(e) => {
                      e.preventDefault();
                      // Navigate using window.location for now (TanStack Router will replace this)
                      window.location.pathname = item.href;
                    }}
                  >
                    <span className={styles.navItemIcon} aria-hidden="true">
                      <Icon />
                    </span>
                    <span className={styles.navItemLabel}>{item.label}</span>
                  </a>
                );
              })}
            </div>
          ))}
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

  // Use a simple state tracking approach
  const getIsMobile = () => queryRef.current?.matches ?? false;

  // We need state to trigger re-renders
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


