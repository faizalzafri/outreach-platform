import type { Tenant, TenantMembership } from '@/types/tenant';

let counter = 0;

export function buildTenant(overrides: Partial<Tenant> = {}): Tenant {
  counter += 1;
  return {
    id: `tenant-${counter}`,
    name: `Test Tenant ${counter}`,
    slug: `test-tenant-${counter}`,
    status: 'ACTIVE',
    createdDate: '2025-01-15T10:00:00Z',
    ...overrides,
  };
}

export function buildTenantMembership(overrides: Partial<TenantMembership> = {}): TenantMembership {
  counter += 1;
  return {
    tenantId: `tenant-${counter}`,
    tenantName: `Test Tenant ${counter}`,
    tenantStatus: 'ACTIVE',
    role: 'ADMIN',
    ...overrides,
  };
}
