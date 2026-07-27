/**
 * ProtectedRoute Component
 *
 * Route guard that checks if the current user has at least one of the required roles.
 * - If authorized → renders children
 * - If unauthorized → renders 403 Forbidden page
 * - If roles are still loading → renders a loading indicator
 *
 * Requirements: 9.2, 9.3, 9.8, 9.9
 */

import { usePermission } from '@/hooks/usePermission';
import { ForbiddenPage } from './ForbiddenPage';

export interface ProtectedRouteProps {
  requiredRoles: string[];
  children: React.ReactNode;
}

export function ProtectedRoute({ requiredRoles, children }: ProtectedRouteProps) {
  const { hasPermission, isLoading } = usePermission(requiredRoles);

  if (isLoading) {
    return (
      <div
        role="status"
        aria-label="Verifying permissions"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '40vh',
        }}
      >
        <span>Verifying access...</span>
      </div>
    );
  }

  if (!hasPermission) {
    return <ForbiddenPage />;
  }

  return <>{children}</>;
}
