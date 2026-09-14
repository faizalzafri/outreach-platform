/**
 * TenantSelector
 *
 * Platform Admin only: a searchable dropdown for switching which tenant's data the admin is
 * viewing. Selecting a tenant sets adminSelectedTenantId in the Tenant Store and invalidates every
 * tenant-scoped query so the UI refetches under the new tenant instead of showing stale data.
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { useDebounce } from '@/hooks/useDebounce';
import { useTenantList } from '@/hooks/useTenant';
import { useTenantStore } from '@/stores/tenant-store';
import { truncateName } from '@/lib/tenant-utils';
import type { TenantStatus } from '@/types/tenant';
import styles from './TenantSelector.module.css';

const TRIGGER_LABEL_MAX_LENGTH = 30;
const PAGE_SIZE = 50;
const ALL_TENANTS_LABEL = 'All Tenants';

const STATUS_LABELS: Record<TenantStatus, string> = {
  ACTIVE: 'Active',
  SUSPENDED: 'Suspended',
  DEACTIVATED: 'Deactivated',
};

export function TenantSelector() {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 300);
  const containerRef = useRef<HTMLDivElement>(null);

  const adminSelectedTenantId = useTenantStore((state) => state.adminSelectedTenantId);
  const setAdminSelectedTenant = useTenantStore((state) => state.setAdminSelectedTenant);
  const queryClient = useQueryClient();

  const { data, isLoading, isError, refetch } = useTenantList(
    { search: debouncedSearch || undefined, page: 0, size: PAGE_SIZE },
    isOpen,
  );

  // The tenant list only fetches while the panel is open (see `enabled` below), so right after a
  // fresh page load with an existing admin override — before the panel has ever been opened —
  // this falls back to the raw tenant ID until the admin opens the dropdown once. Same
  // graceful-degradation trade-off as TenantBadge's error fallback, not worth a dedicated
  // single-tenant lookup query just for this rare edge case.
  const selectedTenant = data?.content.find((t) => t.id === adminSelectedTenantId);
  const triggerLabel = adminSelectedTenantId
    ? truncateName(selectedTenant?.name ?? adminSelectedTenantId, TRIGGER_LABEL_MAX_LENGTH)
    : ALL_TENANTS_LABEL;

  const close = useCallback(() => {
    setIsOpen(false);
    setSearch('');
  }, []);

  // Close on click outside.
  useEffect(() => {
    if (!isOpen) return;

    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        close();
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen, close]);

  // Close on Escape.
  useEffect(() => {
    if (!isOpen) return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        close();
      }
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, close]);

  function selectTenant(tenantId: string | null) {
    setAdminSelectedTenant(tenantId);
    // Every tenant-scoped query key starts with its own domain segment followed by a tenantId
    // segment (see query-keys.ts) — invalidating everything is simpler and safer here than trying
    // to enumerate each domain, and a full-app tenant switch is rare enough that the extra
    // refetching cost doesn't matter.
    void queryClient.invalidateQueries();
    close();
  }

  return (
    <div className={styles.container} ref={containerRef}>
      <button
        type="button"
        className={styles.trigger}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((open) => !open)}
        title={selectedTenant?.name ?? (adminSelectedTenantId ?? undefined)}
      >
        <span className={styles.triggerLabel}>{triggerLabel}</span>
        <svg className={styles.chevron} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" d="M6 9l6 6 6-6" />
        </svg>
      </button>

      {isOpen && (
        <div className={styles.panel} role="listbox" aria-label="Select tenant">
          <input
            type="text"
            className={styles.searchInput}
            placeholder="Search tenants..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            autoFocus
          />

          {isError ? (
            <div className={styles.stateMessage}>
              <p>Could not load tenants.</p>
              <button type="button" className={styles.retryButton} onClick={() => refetch()}>
                Retry
              </button>
            </div>
          ) : isLoading ? (
            <div className={styles.stateMessage}>
              <span className={styles.spinner} aria-label="Loading tenants" />
            </div>
          ) : (
            <ul className={styles.list}>
              <li>
                <button
                  type="button"
                  role="option"
                  aria-selected={adminSelectedTenantId === null}
                  className={styles.option}
                  onClick={() => selectTenant(null)}
                >
                  {ALL_TENANTS_LABEL}
                </button>
              </li>
              {data?.content.map((tenant) => (
                <li key={tenant.id}>
                  <button
                    type="button"
                    role="option"
                    aria-selected={adminSelectedTenantId === tenant.id}
                    className={styles.option}
                    onClick={() => selectTenant(tenant.id)}
                  >
                    <span className={styles.optionName}>{tenant.name}</span>
                    <span className={`${styles.statusBadge} ${styles[tenant.status.toLowerCase()]}`}>
                      {STATUS_LABELS[tenant.status]}
                    </span>
                  </button>
                </li>
              ))}
              {data?.content.length === 0 && <li className={styles.empty}>No tenants found</li>}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
