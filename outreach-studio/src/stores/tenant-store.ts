import { create } from 'zustand';

import type { TenantJwtClaims } from '@/types/tenant';

export interface TenantState {
  tenantId: string | null;
  tenantName: string | null;
  tenantRoles: string[];
  isPlatformAdmin: boolean;
  /** Platform Admin's chosen tenant override — null means "no override, use tenantId". */
  adminSelectedTenantId: string | null;

  /** Maps decoded JWT tenant claims onto state. Platform Admins with no tenant_id get null/empty. */
  setTenantFromJwt: (claims: TenantJwtClaims) => void;
  setTenantName: (name: string | null) => void;
  setAdminSelectedTenant: (tenantId: string | null) => void;
  clearTenant: () => void;

  /**
   * The tenant ID that should actually be used for API calls and cache keys:
   * the Platform Admin's override if one is set, else the token's own tenant.
   * A plain method (not a derived field) so non-component code — HTTP
   * interceptors, query key factories — can call
   * `useTenantStore.getState().getEffectiveTenantId()` synchronously.
   */
  getEffectiveTenantId: () => string | null;
}

export const useTenantStore = create<TenantState>()((set, get) => ({
  tenantId: null,
  tenantName: null,
  tenantRoles: [],
  isPlatformAdmin: false,
  adminSelectedTenantId: null,

  setTenantFromJwt: (claims) =>
    set({
      tenantId: claims.tenant_id ?? null,
      tenantRoles: claims.tenant_roles ?? [],
      isPlatformAdmin: claims.platform_admin ?? false,
    }),

  setTenantName: (name) => set({ tenantName: name }),

  setAdminSelectedTenant: (tenantId) => set({ adminSelectedTenantId: tenantId }),

  clearTenant: () =>
    set({
      tenantId: null,
      tenantName: null,
      tenantRoles: [],
      isPlatformAdmin: false,
      adminSelectedTenantId: null,
    }),

  getEffectiveTenantId: () => get().adminSelectedTenantId ?? get().tenantId,
}));
