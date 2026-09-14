import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useDebounce } from '@/hooks/useDebounce';
import { useEffectiveTenantId } from '@/hooks/useTenant';
import type { PageResponse } from '@/types/api';
import type { Team, TeamMember, TeamListParams, TeamMemberListParams } from '@/types/tenant';

export interface AvailableUser {
  id: string;
  username: string;
  email: string;
}

export function useTeamList(params: TeamListParams = {}) {
  const tenantId = useEffectiveTenantId();
  return useQuery<PageResponse<Team>>({
    queryKey: queryKeys.teams.list(tenantId, params),
    queryFn: async () => {
      const response = await httpClient.get<PageResponse<Team>>('/teams', { params });
      return response.data;
    },
  });
}

export function useTeamDetail(teamId: string) {
  const tenantId = useEffectiveTenantId();
  return useQuery<Team>({
    queryKey: queryKeys.teams.detail(tenantId, teamId),
    queryFn: async () => {
      const response = await httpClient.get<Team>(`/teams/${teamId}`);
      return response.data;
    },
    enabled: Boolean(teamId),
  });
}

export function useTeamMembers(teamId: string, params: TeamMemberListParams = {}) {
  const tenantId = useEffectiveTenantId();
  return useQuery<PageResponse<TeamMember>>({
    queryKey: queryKeys.teams.members(tenantId, teamId, params),
    queryFn: async () => {
      const response = await httpClient.get<PageResponse<TeamMember>>(`/teams/${teamId}/members`, {
        params,
      });
      return response.data;
    },
    enabled: Boolean(teamId),
  });
}

export function useCreateTeam() {
  const queryClient = useQueryClient();
  const tenantId = useEffectiveTenantId();
  return useMutation<Team, unknown, { name: string; description?: string }>({
    mutationFn: async (values) => {
      const response = await httpClient.post<Team>('/teams', values);
      return response.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams.all(tenantId) });
    },
  });
}

export function useUpdateTeam(teamId: string) {
  const queryClient = useQueryClient();
  const tenantId = useEffectiveTenantId();
  return useMutation<Team, unknown, { name: string; description?: string }>({
    mutationFn: async (values) => {
      const response = await httpClient.put<Team>(`/teams/${teamId}`, values);
      return response.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams.all(tenantId) });
    },
  });
}

export function useDeleteTeam() {
  const queryClient = useQueryClient();
  const tenantId = useEffectiveTenantId();
  return useMutation<void, unknown, string>({
    mutationFn: async (teamId) => {
      await httpClient.delete(`/teams/${teamId}`);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams.all(tenantId) });
    },
  });
}

export function useAddTeamMember(teamId: string) {
  const queryClient = useQueryClient();
  const tenantId = useEffectiveTenantId();
  return useMutation<TeamMember, unknown, string>({
    mutationFn: async (userId) => {
      const response = await httpClient.post<TeamMember>(`/teams/${teamId}/members`, { userId });
      return response.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams.detail(tenantId, teamId) });
      void queryClient.invalidateQueries({
        queryKey: [...queryKeys.teams.detail(tenantId, teamId), 'members'],
      });
    },
  });
}

export function useRemoveTeamMember(teamId: string) {
  const queryClient = useQueryClient();
  const tenantId = useEffectiveTenantId();
  return useMutation<void, unknown, string>({
    mutationFn: async (userId) => {
      await httpClient.delete(`/teams/${teamId}/members/${userId}`);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams.detail(tenantId, teamId) });
      void queryClient.invalidateQueries({
        queryKey: [...queryKeys.teams.detail(tenantId, teamId), 'members'],
      });
    },
  });
}

/** Searchable user selector for "add team member" — debounced 300ms, requires 2+ chars. */
export function useAvailableUsers(teamId: string, search: string) {
  const debouncedSearch = useDebounce(search, 300);
  const trimmed = debouncedSearch.trim();
  const tenantId = useEffectiveTenantId();

  return useQuery<AvailableUser[]>({
    queryKey: [...queryKeys.teams.detail(tenantId, teamId), 'available-users', trimmed],
    queryFn: async () => {
      const response = await httpClient.get<AvailableUser[]>(`/teams/${teamId}/available-users`, {
        params: { search: trimmed || undefined },
      });
      return response.data.slice(0, 20);
    },
    enabled: Boolean(teamId) && trimmed.length >= 2,
  });
}
