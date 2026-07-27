import type { User } from '@/types/domain';

let counter = 0;

export function buildUser(overrides: Partial<User> = {}): User {
  counter += 1;
  return {
    id: `user-${counter}`,
    username: `testuser${counter}`,
    email: `testuser${counter}@example.com`,
    role: 'ROLE_PMO',
    enabled: true,
    ...overrides,
  };
}
