/**
 * Tenant Selection Route
 *
 * Shown after login when the user belongs to more than one ACTIVE tenant and hasn't explicitly
 * chosen one yet (the auth server's tenant_selection_required claim). Selecting a tenant records
 * the choice server-side, then a silent token refresh picks up the new tenant_id claim before
 * continuing into the app.
 */

import { useEffect, useState } from 'react';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { authModule } from '@/lib/auth';
import { useTenantMemberships, useSelectTenant } from '@/hooks/useTenant';
import type { TenantMembership } from '@/types/tenant';
import styles from './login.module.css';
import selectTenantStyles from './select-tenant.module.css';

const LAST_TENANT_STORAGE_KEY = 'outreach-last-tenant';

export const Route = createFileRoute('/select-tenant')({
  component: SelectTenantPage,
});

export function SelectTenantPage() {
  const navigate = useNavigate();
  const { data: memberships, isLoading, isError, refetch } = useTenantMemberships();
  const selectTenant = useSelectTenant();
  const [selectingId, setSelectingId] = useState<string | null>(null);

  async function chooseTenant(tenantId: string) {
    setSelectingId(tenantId);
    try {
      await selectTenant.mutateAsync(tenantId);
      localStorage.setItem(LAST_TENANT_STORAGE_KEY, tenantId);
      // The auth server only re-resolves the active tenant during token issuance, so the
      // just-recorded selection isn't reflected until this refresh completes.
      await authModule.silentRefresh();
      void navigate({ to: '/' });
    } catch {
      setSelectingId(null);
    }
  }

  const activeMemberships = (memberships ?? []).filter((m) => m.tenantStatus === 'ACTIVE');
  const lastUsedId = typeof window !== 'undefined' ? localStorage.getItem(LAST_TENANT_STORAGE_KEY) : null;

  // Auto-select when there's only one real choice to make. Deferred to a microtask so the
  // effect body itself never synchronously triggers the state update inside chooseTenant.
  useEffect(() => {
    if (activeMemberships.length === 1 && !selectingId && !selectTenant.isPending) {
      const tenantId = activeMemberships[0]!.tenantId;
      queueMicrotask(() => void chooseTenant(tenantId));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only re-run when the membership list itself changes
  }, [activeMemberships.length]);

  const sortedMemberships = [...(memberships ?? [])].sort((a, b) => {
    if (a.tenantId === lastUsedId) return -1;
    if (b.tenantId === lastUsedId) return 1;
    return 0;
  });

  return (
    <div className={styles.page}>
      <div className={`${styles.card} ${selectTenantStyles.wideCard}`}>
        <div className={styles.brand}>
          <span className={styles.brandIcon} aria-hidden="true">
            ◈
          </span>
          <span className={styles.brandName}>Outreach Studio</span>
        </div>

        <h1 className={styles.heading}>Select an Organization</h1>
        <p className={styles.subtitle}>Choose which organization you'd like to work in.</p>

        {isError && (
          <div className={selectTenantStyles.errorBox} role="alert">
            <p>Could not load your organizations.</p>
            <button type="button" className={styles.button} onClick={() => refetch()}>
              Retry
            </button>
          </div>
        )}

        {selectTenant.isError && (
          <div className={selectTenantStyles.errorBox} role="alert">
            <p>Could not select that organization. Please try again.</p>
          </div>
        )}

        {isLoading && (
          <div className={styles.redirecting} role="status" aria-label="Loading organizations">
            <div className={styles.spinner} />
          </div>
        )}

        {!isLoading && !isError && (
          <ul className={selectTenantStyles.list}>
            {sortedMemberships.map((membership) => (
              <TenantCard
                key={membership.tenantId}
                membership={membership}
                isLastUsed={membership.tenantId === lastUsedId}
                isSelecting={selectingId === membership.tenantId}
                disabled={selectTenant.isPending}
                onSelect={() => chooseTenant(membership.tenantId)}
              />
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

interface TenantCardProps {
  membership: TenantMembership;
  isLastUsed: boolean;
  isSelecting: boolean;
  disabled: boolean;
  onSelect: () => void;
}

function TenantCard({ membership, isLastUsed, isSelecting, disabled, onSelect }: TenantCardProps) {
  const isSelectable = membership.tenantStatus === 'ACTIVE';

  return (
    <li>
      <button
        type="button"
        className={`${selectTenantStyles.tenantCard} ${!isSelectable ? selectTenantStyles.disabledCard : ''}`}
        onClick={onSelect}
        disabled={!isSelectable || disabled}
      >
        <div className={selectTenantStyles.tenantCardHeader}>
          <span className={selectTenantStyles.tenantName}>{membership.tenantName}</span>
          {isLastUsed && <span className={selectTenantStyles.lastUsedLabel}>Last used</span>}
        </div>
        <div className={selectTenantStyles.tenantCardMeta}>
          <span
            className={`${selectTenantStyles.statusBadge} ${selectTenantStyles[membership.tenantStatus.toLowerCase()]}`}
          >
            {membership.tenantStatus}
          </span>
          <span className={selectTenantStyles.roleLabel}>{membership.role}</span>
        </div>
        {isSelecting && <span className={selectTenantStyles.selectingSpinner} aria-label="Selecting" />}
      </button>
    </li>
  );
}
