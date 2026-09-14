/**
 * Layout Shell Component
 *
 * Composes the Sidebar, Header, and main content area into a cohesive layout.
 * Uses semantic HTML elements (nav, header, main) for accessibility.
 * Manages sidebar state via Zustand UI store and auth via useAuth hook.
 */

import { useMemo } from 'react';
import { useRouterState } from '@tanstack/react-router';
import { useUIStore } from '@/stores/ui-store';
import { useAuth } from '@/hooks/useAuth';
import { filterNavigationByRoles } from '@/lib/navigation-filter';
import { Sidebar, type NavigationGroup } from './Sidebar';
import { Header } from './Header';
import styles from './LayoutShell.module.css';

export interface LayoutShellProps {
  children: React.ReactNode;
}

// Simple SVG icon components for navigation
function DashboardIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <rect x="3" y="3" width="7" height="7" rx="1" />
      <rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" />
      <rect x="14" y="14" width="7" height="7" rx="1" />
    </svg>
  );
}

function EventsIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  );
}

function VolunteersIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 00-3-3.87" />
      <path d="M16 3.13a4 4 0 010 7.75" />
    </svg>
  );
}

function FeedbackIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
    </svg>
  );
}

function IngestionIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
      <polyline points="7 10 12 15 17 10" />
      <line x1="12" y1="15" x2="12" y2="3" />
    </svg>
  );
}

function NotificationsIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 01-3.46 0" />
    </svg>
  );
}

function ReportsIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <line x1="18" y1="20" x2="18" y2="10" />
      <line x1="12" y1="20" x2="12" y2="4" />
      <line x1="6" y1="20" x2="6" y2="14" />
    </svg>
  );
}

function AdminIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z" />
    </svg>
  );
}

function AiInsightsIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M12 2a2 2 0 012 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 017 7h1a1 1 0 011 1v3a1 1 0 01-1 1h-1a7 7 0 01-7 7h-2a7 7 0 01-7-7H4a1 1 0 01-1-1v-3a1 1 0 011-1h1a7 7 0 017-7h1V5.73A2 2 0 0111 4a2 2 0 011-2z" />
      <circle cx="9" cy="13" r="1" />
      <circle cx="15" cy="13" r="1" />
    </svg>
  );
}

function TeamsIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 00-3-3.87" />
      <path d="M16 3.13a4 4 0 010 7.75" />
    </svg>
  );
}

function AuditLogIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="16" y1="13" x2="8" y2="13" />
      <line x1="16" y1="17" x2="8" y2="17" />
      <polyline points="10 9 9 9 8 9" />
    </svg>
  );
}

/**
 * Default navigation groups for the Outreach Studio sidebar.
 *
 * Role-based visibility rules:
 * - ROLE_ADMIN: all sections
 * - ROLE_PMO: Dashboard, Events, Volunteers, Feedback, Reports
 * - ROLE_POC: Dashboard, Events (assigned), Volunteers (enrolled), Feedback
 *
 * Items with empty requiredRoles are visible to all authenticated users.
 */
const navigationGroups: NavigationGroup[] = [
  {
    label: 'Overview',
    items: [
      { label: 'Dashboard', href: '/dashboard', icon: DashboardIcon, requiredRoles: [] },
    ],
  },
  {
    label: 'Management',
    items: [
      { label: 'Events', href: '/events', icon: EventsIcon, requiredRoles: ['ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC'] },
      { label: 'Volunteers', href: '/volunteers', icon: VolunteersIcon, requiredRoles: ['ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC'] },
      { label: 'Feedback', href: '/feedback', icon: FeedbackIcon, requiredRoles: ['ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC'] },
    ],
  },
  {
    label: 'Operations',
    items: [
      { label: 'Ingestion', href: '/ingestion', icon: IngestionIcon, requiredRoles: ['ROLE_ADMIN'] },
      { label: 'Notifications', href: '/notifications', icon: NotificationsIcon, requiredRoles: ['ROLE_ADMIN'] },
    ],
  },
  {
    label: 'Analytics',
    items: [
      { label: 'Reports', href: '/reports', icon: ReportsIcon, requiredRoles: ['ROLE_ADMIN', 'ROLE_PMO'] },
      { label: 'AI Insights', href: '/ai-insights', icon: AiInsightsIcon, requiredRoles: ['ROLE_ADMIN'] },
    ],
  },
  {
    label: 'System',
    items: [
      { label: 'Teams', href: '/teams', icon: TeamsIcon, requiredRoles: ['ROLE_ADMIN'] },
      { label: 'Administration', href: '/admin', icon: AdminIcon, requiredRoles: ['ROLE_ADMIN'] },
      { label: 'Audit Log', href: '/audit-log', icon: AuditLogIcon, requiredRoles: ['ROLE_ADMIN'] },
    ],
  },
];

export function LayoutShell({ children }: LayoutShellProps) {
  const sidebarCollapsed = useUIStore((state) => state.sidebarCollapsed);
  const toggleSidebar = useUIStore((state) => state.toggleSidebar);
  const { user, logout, isLoading } = useAuth();

  // Get active route from TanStack Router's state
  const routerState = useRouterState();
  const activeRoute = routerState.location.pathname;

  const handleLogout = () => {
    void logout();
  };

  // Provide a fallback user for rendering if not yet authenticated
  const displayUser = user ?? { sub: '', name: 'User', email: '', roles: [] };

  // Filter navigation by user roles (multi-role users get union of permissions)
  const filteredNavigationGroups = useMemo(
    () => filterNavigationByRoles(navigationGroups, user?.roles ?? []),
    [user?.roles]
  );

  const mainClasses = [
    styles.mainWrapper,
    sidebarCollapsed ? styles.sidebarCollapsed : '',
  ]
    .filter(Boolean)
    .join(' ');

  // Show loading state while roles are resolving
  if (isLoading) {
    return (
      <div className={styles.layout}>
        <div
          role="status"
          aria-label="Loading navigation"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100vh',
            width: '100%',
          }}
        >
          <span>Loading...</span>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.layout}>
      {/* Skip navigation — first focusable element */}
      <a href="#main-content" className="skip-nav">
        Skip to main content
      </a>

      <Sidebar
        collapsed={sidebarCollapsed}
        onToggle={toggleSidebar}
        navigationGroups={filteredNavigationGroups}
        activeRoute={activeRoute}
        user={displayUser}
        onLogout={handleLogout}
      />

      <div className={mainClasses}>
        <Header onToggleSidebar={toggleSidebar} />

        <main className={styles.main} id="main-content" role="main">
          {children}
        </main>
      </div>
    </div>
  );
}
