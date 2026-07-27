/**
 * Navigation Filter Utility
 *
 * Filters navigation groups based on user roles.
 * Multi-role users get the union of all permissions.
 */

import type { NavigationGroup } from '@/components/layout/Sidebar';

/**
 * Filters navigation groups and items by the user's assigned roles.
 *
 * Rules:
 * - Items with empty `requiredRoles` are visible to all authenticated users.
 * - Items with non-empty `requiredRoles` require at least one matching role.
 * - Groups with no visible items after filtering are excluded.
 */
export function filterNavigationByRoles(
  groups: NavigationGroup[],
  userRoles: string[]
): NavigationGroup[] {
  return groups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => {
        // If no roles required, always visible
        if (item.requiredRoles.length === 0) {
          return true;
        }
        // User must hold at least one of the required roles
        return item.requiredRoles.some((role) => userRoles.includes(role));
      }),
    }))
    .filter((group) => group.items.length > 0);
}
