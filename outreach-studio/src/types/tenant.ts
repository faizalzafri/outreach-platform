// Multi-tenancy, team, sharing, and activity feed types for the Outreach Studio SPA

export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED';

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: TenantStatus;
  createdAt: string;
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
  createdAt: string;
}

export interface TeamMember {
  userId: string;
  name: string;
  email: string;
  role: string;
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

export type ActivityActionType = 'created' | 'modified' | 'shared' | 'activated';
export type ActivityResourceType = 'sequence' | 'template' | 'contact_list';

export interface ActivityEvent {
  id: string;
  actorName: string;
  actionType: ActivityActionType;
  resourceType: ActivityResourceType;
  resourceName: string;
  timestamp: string;
  teamName?: string;
}

export interface ActivityFeedResponse {
  items: ActivityEvent[];
  nextCursor: string | null;
  hasMore: boolean;
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

export interface ActivityFilters {
  teamId?: string;
  actionTypes?: ActivityActionType[];
  resourceTypes?: ActivityResourceType[];
  startDate?: string;
  endDate?: string;
}
