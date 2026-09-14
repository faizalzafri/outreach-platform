import { describe, it, expect, beforeEach } from 'vitest';
import { useTenantStore } from '../tenant-store';

describe('Tenant Store', () => {
  beforeEach(() => {
    useTenantStore.setState({
      tenantId: null,
      tenantName: null,
      tenantRoles: [],
      isPlatformAdmin: false,
      adminSelectedTenantId: null,
    });
  });

  describe('setTenantFromJwt', () => {
    it('maps tenant claims onto state for a regular tenant user', () => {
      useTenantStore.getState().setTenantFromJwt({
        tenant_id: 'tenant-123',
        tenant_roles: ['ADMIN'],
        platform_admin: false,
      });

      const state = useTenantStore.getState();
      expect(state.tenantId).toBe('tenant-123');
      expect(state.tenantRoles).toEqual(['ADMIN']);
      expect(state.isPlatformAdmin).toBe(false);
    });

    it('sets null tenantId and empty roles for a Platform Admin with no tenant_id', () => {
      useTenantStore.getState().setTenantFromJwt({
        platform_admin: true,
      });

      const state = useTenantStore.getState();
      expect(state.tenantId).toBeNull();
      expect(state.tenantRoles).toEqual([]);
      expect(state.isPlatformAdmin).toBe(true);
    });
  });

  describe('setTenantName', () => {
    it('sets the tenant name', () => {
      useTenantStore.getState().setTenantName('Acme Corp');
      expect(useTenantStore.getState().tenantName).toBe('Acme Corp');
    });

    it('clears the tenant name when passed null', () => {
      useTenantStore.getState().setTenantName('Acme Corp');
      useTenantStore.getState().setTenantName(null);
      expect(useTenantStore.getState().tenantName).toBeNull();
    });
  });

  describe('setAdminSelectedTenant / getEffectiveTenantId', () => {
    it('effective tenant is the token tenant when no admin override is set', () => {
      useTenantStore.getState().setTenantFromJwt({ tenant_id: 'tenant-a', platform_admin: false });
      expect(useTenantStore.getState().getEffectiveTenantId()).toBe('tenant-a');
    });

    it('effective tenant is the admin override when one is set', () => {
      useTenantStore.getState().setTenantFromJwt({ platform_admin: true });
      useTenantStore.getState().setAdminSelectedTenant('tenant-b');
      expect(useTenantStore.getState().getEffectiveTenantId()).toBe('tenant-b');
    });

    it('clearing the admin override falls back to the token tenant', () => {
      useTenantStore.getState().setTenantFromJwt({ tenant_id: 'tenant-a', platform_admin: false });
      useTenantStore.getState().setAdminSelectedTenant('tenant-b');
      useTenantStore.getState().setAdminSelectedTenant(null);
      expect(useTenantStore.getState().getEffectiveTenantId()).toBe('tenant-a');
    });
  });

  describe('clearTenant', () => {
    it('resets all tenant state to defaults', () => {
      useTenantStore.getState().setTenantFromJwt({
        tenant_id: 'tenant-a',
        tenant_roles: ['ADMIN'],
        platform_admin: false,
      });
      useTenantStore.getState().setTenantName('Acme Corp');
      useTenantStore.getState().setAdminSelectedTenant('tenant-b');

      useTenantStore.getState().clearTenant();

      const state = useTenantStore.getState();
      expect(state.tenantId).toBeNull();
      expect(state.tenantName).toBeNull();
      expect(state.tenantRoles).toEqual([]);
      expect(state.isPlatformAdmin).toBe(false);
      expect(state.adminSelectedTenantId).toBeNull();
    });
  });
});
