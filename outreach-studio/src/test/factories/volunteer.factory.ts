import type { Volunteer } from '@/types/domain';

let counter = 0;

export function buildVolunteer(overrides: Partial<Volunteer> = {}): Volunteer {
  counter += 1;
  return {
    employeeId: `emp-${counter}`,
    name: `Volunteer ${counter}`,
    email: `volunteer${counter}@example.com`,
    department: 'Engineering',
    location: 'Bangalore',
    skills: ['communication', 'teamwork'],
    joinDate: '2024-01-15',
    availability: 'AVAILABLE',
    totalEvents: 5,
    averageScore: 4.0,
    ...overrides,
  };
}
