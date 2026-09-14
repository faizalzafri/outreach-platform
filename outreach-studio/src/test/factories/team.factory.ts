import type { Team, TeamMember } from '@/types/tenant';

let counter = 0;

export function buildTeam(overrides: Partial<Team> = {}): Team {
  counter += 1;
  return {
    id: `team-${counter}`,
    name: `Test Team ${counter}`,
    description: 'A test team',
    memberCount: 0,
    createdDate: '2025-01-15T10:00:00Z',
    ...overrides,
  };
}

export function buildTeamMember(overrides: Partial<TeamMember> = {}): TeamMember {
  counter += 1;
  return {
    userId: `user-${counter}`,
    username: `test_user_${counter}`,
    email: `test_user_${counter}@example.com`,
    joinedAt: '2025-01-20T10:00:00Z',
    ...overrides,
  };
}
