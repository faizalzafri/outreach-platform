/**
 * TenantBadge
 *
 * Displays the current tenant's name in the header, for regular (non-Platform-Admin) users.
 * Falls back to a truncated tenant ID with a warning toast if the name fetch fails — the tenant
 * badge should never block the rest of the UI from rendering.
 */

import { useEffect } from 'react';

import { useTenantName } from '@/hooks/useTenant';
import { useTenantStore } from '@/stores/tenant-store';
import { useToast } from '@/hooks/useToast';
import styles from './TenantBadge.module.css';

const ERROR_TOAST_DURATION_MS = 8_000;

// Hoisted outside the component so it isn't re-created on every render.
const ORG_ICON = (
  <svg
    className={styles.icon}
    width="16"
    height="16"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth={2}
    aria-hidden="true"
  >
    <path strokeLinecap="round" strokeLinejoin="round" d="M3 21h18M5 21V7l7-4 7 4v14M9 9h1m4 0h1m-6 4h1m4 0h1m-6 4h1m4 0h1" />
  </svg>
);

export function TenantBadge() {
  const tenantId = useTenantStore((state) => state.tenantId);
  const { data, isLoading, isError } = useTenantName();
  const { toast } = useToast();

  useEffect(() => {
    if (isError) {
      toast({
        severity: 'warning',
        message: 'Could not load tenant name — showing tenant ID instead.',
        durationMs: ERROR_TOAST_DURATION_MS,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only re-fire when the error state itself flips
  }, [isError]);

  if (isLoading) {
    return (
      <div className={styles.badge} aria-hidden="true">
        {ORG_ICON}
        <span className={styles.skeleton} />
      </div>
    );
  }

  if (isError) {
    const fallback = tenantId ? tenantId.slice(0, 8) : 'unknown';
    return (
      <div className={styles.badge} role="status" title={tenantId ?? undefined}>
        {ORG_ICON}
        <span className={styles.name}>{fallback}</span>
      </div>
    );
  }

  const name = data?.name ?? '';
  return (
    <div className={styles.badge} role="status" title={name}>
      {ORG_ICON}
      <span className={styles.name}>{name}</span>
    </div>
  );
}
