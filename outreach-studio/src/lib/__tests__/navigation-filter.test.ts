import { describe, it, expect } from 'vitest';
import { filterNavigationByRoles } from '../navigation-filter';
import type { NavigationGroup } from '@/components/layout/Sidebar';

// Minimal icon stub for NavigationItem
const StubIcon = () => null;

function createNavGroup(label: string, items: Array<{ label: string; href: string; requiredRoles: string[] }>): NavigationGroup {
  return {
    label,
    items: items.map((item) => ({ ...item, icon: StubIcon })),
  };
}

describe('filterNavigationByRoles', () => {
  const allGroups: NavigationGroup[] = [
    createNavGroup('Overview', [
      { label: 'Dashboard', href: '/dashboard', requiredRoles: ['ROLE_ADMIN', 'ROLE_PMO'] },
    ]),
    createNavGroup('Management', [
      { label: 'Events', href: '/events', requiredRoles: ['ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC'] },
      { label: 'Volunteers', href: '/volunteers', requiredRoles: ['ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC'] },
      { label: 'Feedback', href: '/feedback', requiredRoles: ['ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC'] },
    ]),
    createNavGroup('Operations', [
      { label: 'Ingestion', href: '/ingestion', requiredRoles: ['ROLE_ADMIN'] },
      { label: 'Notifications', href: '/notifications', requiredRoles: ['ROLE_ADMIN'] },
      { label: 'Reports', href: '/reports', requiredRoles: ['ROLE_ADMIN', 'ROLE_PMO'] },
    ]),
    createNavGroup('System', [
      { label: 'Administration', href: '/admin', requiredRoles: ['ROLE_ADMIN'] },
      { label: 'Audit Log', href: '/audit-log', requiredRoles: ['ROLE_ADMIN'] },
    ]),
  ];

  it('ROLE_ADMIN sees all navigation items', () => {
    const result = filterNavigationByRoles(allGroups, ['ROLE_ADMIN']);

    expect(result).toHaveLength(4);
    expect(result[0].items).toHaveLength(1); // Dashboard
    expect(result[1].items).toHaveLength(3); // Events, Volunteers, Feedback
    expect(result[2].items).toHaveLength(3); // Ingestion, Notifications, Reports
    expect(result[3].items).toHaveLength(2); // Administration, Audit Log
  });

  it('ROLE_PMO sees Dashboard, Events, Volunteers, Feedback, Reports', () => {
    const result = filterNavigationByRoles(allGroups, ['ROLE_PMO']);

    // Flatten all visible item labels
    const visibleLabels = result.flatMap((g) => g.items.map((i) => i.label));

    expect(visibleLabels).toContain('Dashboard');
    expect(visibleLabels).toContain('Events');
    expect(visibleLabels).toContain('Volunteers');
    expect(visibleLabels).toContain('Feedback');
    expect(visibleLabels).toContain('Reports');
    expect(visibleLabels).not.toContain('Ingestion');
    expect(visibleLabels).not.toContain('Notifications');
    expect(visibleLabels).not.toContain('Administration');
    expect(visibleLabels).not.toContain('Audit Log');
  });

  it('ROLE_POC sees Events, Volunteers, Feedback only', () => {
    const result = filterNavigationByRoles(allGroups, ['ROLE_POC']);

    const visibleLabels = result.flatMap((g) => g.items.map((i) => i.label));

    expect(visibleLabels).toContain('Events');
    expect(visibleLabels).toContain('Volunteers');
    expect(visibleLabels).toContain('Feedback');
    expect(visibleLabels).not.toContain('Dashboard');
    expect(visibleLabels).not.toContain('Reports');
    expect(visibleLabels).not.toContain('Administration');
    expect(visibleLabels).not.toContain('Ingestion');
  });

  it('excludes groups with no visible items after filtering', () => {
    const result = filterNavigationByRoles(allGroups, ['ROLE_POC']);

    // POC should not see "Overview" (Dashboard) or "System" (Admin, Audit) or "Operations" (Ingestion, Notifications, Reports)
    const groupLabels = result.map((g) => g.label);
    expect(groupLabels).not.toContain('Overview');
    expect(groupLabels).not.toContain('System');
    expect(groupLabels).not.toContain('Operations');
    expect(groupLabels).toContain('Management');
  });

  it('items with empty requiredRoles are visible to all authenticated users', () => {
    const groups: NavigationGroup[] = [
      createNavGroup('Public', [
        { label: 'Help', href: '/help', requiredRoles: [] },
      ]),
    ];

    const result = filterNavigationByRoles(groups, ['ROLE_POC']);

    expect(result).toHaveLength(1);
    expect(result[0].items[0].label).toBe('Help');
  });

  it('multi-role users get union of all permissions', () => {
    const result = filterNavigationByRoles(allGroups, ['ROLE_PMO', 'ROLE_POC']);

    const visibleLabels = result.flatMap((g) => g.items.map((i) => i.label));

    // PMO gets Dashboard and Reports, POC gets Events/Volunteers/Feedback
    expect(visibleLabels).toContain('Dashboard');
    expect(visibleLabels).toContain('Events');
    expect(visibleLabels).toContain('Volunteers');
    expect(visibleLabels).toContain('Feedback');
    expect(visibleLabels).toContain('Reports');
    // Neither PMO nor POC gets admin-only items
    expect(visibleLabels).not.toContain('Administration');
    expect(visibleLabels).not.toContain('Audit Log');
  });

  it('returns empty array when user has no roles', () => {
    const groups: NavigationGroup[] = [
      createNavGroup('Restricted', [
        { label: 'Admin Panel', href: '/admin', requiredRoles: ['ROLE_ADMIN'] },
      ]),
    ];

    const result = filterNavigationByRoles(groups, []);

    expect(result).toHaveLength(0);
  });

  it('does not mutate original navigation groups', () => {
    const groups: NavigationGroup[] = [
      createNavGroup('Mixed', [
        { label: 'Public', href: '/public', requiredRoles: [] },
        { label: 'Admin Only', href: '/admin', requiredRoles: ['ROLE_ADMIN'] },
      ]),
    ];

    filterNavigationByRoles(groups, ['ROLE_POC']);

    // Original should still have both items
    expect(groups[0].items).toHaveLength(2);
  });
});
