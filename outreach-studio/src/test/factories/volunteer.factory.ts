import type { Volunteer } from '@/types/domain';

let counter = 0;

export function buildVolunteer(overrides: Partial<Volunteer> = {}): Volunteer {
  counter += 1;
  return {
    id: `vol-${counter}`,
    employeeId: `EMP${String(counter).padStart(3, '0')}`,
    fullName: `Volunteer ${counter}`,
    email: `volunteer${counter}@example.com`,
    phone: '9876543210',
    baseLocation: 'Bangalore',
    department: 'Engineering',
    designation: 'Developer',
    skills: 'Communication,Teamwork',
    availability: 'AVAILABLE',
    totalEventsParticipated: 5,
    avgFeedbackScore: 4.0,
    ...overrides,
  };
}
