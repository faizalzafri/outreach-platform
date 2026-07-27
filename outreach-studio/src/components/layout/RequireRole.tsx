/**
 * RequireRole Component
 *
 * Conditionally renders children based on the user's roles.
 * - If the user holds at least one of the specified roles → renders children
 * - Otherwise → renders nothing (null)
 * - While loading → renders nothing (null) to avoid flicker
 *
 * Useful for showing/hiding UI elements (buttons, sections) based on role.
 *
 * Requirements: 9.4, 9.5, 9.7, 9.8
 */

import type { ReactNode } from 'react';
import { usePermission } from '@/hooks/usePermission';

export interface RequireRoleProps {
  roles: string[];
  children: ReactNode;
}

export function RequireRole({ roles, children }: RequireRoleProps): ReactNode {
  const { hasPermission, isLoading } = usePermission(roles);

  if (isLoading || !hasPermission) {
    return null;
  }

  return <>{children}</>;
}
