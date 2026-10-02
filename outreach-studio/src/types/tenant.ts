// Multi-tenancy, team and sharing types for the Outreach Studio SPA

export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED';

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: TenantStatus;
  /** Named to match the backend's TenantResponse record field exactly (Jackson doesn't rename it). */
  createdDate: string;
}

/** A row in the platform console's organization list (GET /api/tenants). */
export interface TenantSummary extends Tenant {
  memberCount: number;
}

/** One organization in the platform console (GET /api/tenants/{id}). */
export interface TenantDetail extends Tenant {
  lastModifiedDate: string | null;
  lastModifiedBy: string | null;
  membersByRole: Partial<Record<'ADMIN' | 'PMO' | 'POC', number>>;
}

export interface TenantMembership {
  tenantId: string;
  tenantName: string;
  tenantStatus: TenantStatus;
  role: string;
}

export interface Team {
  id: string;
  name: string;
  description: string;
  memberCount: number;
  /** Named to match the backend's TeamResponse record field exactly (Jackson doesn't rename it). */
  createdDate: string;
}

export interface TeamMember {
  userId: string;
  username: string | null;
  email: string | null;
  joinedAt: string;
}

export type Visibility = 'PRIVATE' | 'TEAM' | 'TENANT';
export type PermissionLevel = 'VIEW' | 'EDIT' | 'MANAGE';

export interface ResourcePermission {
  teamId: string;
  teamName: string;
  permissionLevel: PermissionLevel;
}

export interface ResourcePermissions {
  visibility: Visibility;
  teamPermissions: ResourcePermission[];
}

export interface TenantSelectionResponse {
  tenant_selection_required: boolean;
  available_tenants: TenantMembership[];
}

/** JWT claims specific to tenant context — merged into the main JwtClaims interface. */
export interface TenantJwtClaims {
  tenant_id?: string | null;
  tenant_roles?: string[];
  platform_admin?: boolean;
}

export interface TeamListParams {
  page?: number;
  size?: number;
}

export interface TeamMemberListParams {
  page?: number;
  size?: number;
}
