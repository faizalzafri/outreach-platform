import { useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useTenantStore } from '@/stores/tenant-store';
import type { PageResponse } from '@/types/api';
import type { Tenant, TenantMembership } from '@/types/tenant';

/**
 * Fetches the current user's own tenant name and keeps the Tenant Store in sync with it.
 * Falls back to a truncated UUID display (handled by TenantBadge) if the fetch fails — this hook
 * only fetches the *name*, since tenantId/roles already come from the JWT via the auth module.
 */
export function useTenantName() {
  const tenantId = useTenantStore((state) => state.tenantId);
  const setTenantName = useTenantStore((state) => state.setTenantName);

  const query = useQuery<Tenant>({
    queryKey: queryKeys.tenants.current(),
    queryFn: async () => {
      const response = await httpClient.get<Tenant>('/tenants/current');
      return response.data;
    },
    enabled: Boolean(tenantId),
    staleTime: 5 * 60 * 1000,
    retry: 3,
  });

  useEffect(() => {
    if (query.data) {
      setTenantName(query.data.name);
    }
  }, [query.data, setTenantName]);

  return query;
}

export interface TenantListParams {
  search?: string;
  page?: number;
  size?: number;
}

/** Platform Admin only — paginated, searchable tenant list for the Tenant Selector. */
export function useTenantList(params: TenantListParams, enabled = true) {
  return useQuery<PageResponse<Tenant>>({
    queryKey: queryKeys.tenants.list(params),
    queryFn: async () => {
      const response = await httpClient.get<PageResponse<Tenant>>('/tenants', { params });
      return response.data;
    },
    enabled,
  });
}

/** The authenticated user's ACTIVE tenant memberships, for the tenant-selection page. */
export function useTenantMemberships() {
  return useQuery<TenantMembership[]>({
    queryKey: queryKeys.tenants.memberships(),
    queryFn: async () => {
      const response = await httpClient.get<TenantMembership[]>('/auth/tenant-memberships');
      return response.data;
    },
  });
}

/**
 * Records the user's explicit tenant choice. Does not itself return a new token — the caller
 * must follow up with `authModule.silentRefresh()` to obtain one reflecting the selection, since
 * the auth server's token customizer only re-resolves the active tenant during token issuance.
 */
export function useSelectTenant() {
  return useMutation<void, unknown, string>({
    mutationFn: async (tenantId) => {
      await httpClient.post('/auth/select-tenant', { tenantId });
    },
  });
}
