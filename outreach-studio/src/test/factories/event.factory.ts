import type { Event } from '@/types/domain';

let counter = 0;

export function buildEvent(overrides: Partial<Event> = {}): Event {
  counter += 1;
  return {
    id: `evt-${counter}`,
    code: `EVT-${String(counter).padStart(4, '0')}`,
    name: `Test Event ${counter}`,
    description: `Description for test event ${counter}`,
    status: 'DRAFT',
    startDate: '2025-03-01T09:00:00Z',
    endDate: '2025-03-01T17:00:00Z',
    city: 'Bangalore',
    venue: 'Main Hall',
    category: 'Community Service',
    maxVolunteers: 50,
    primaryPoc: 'poc-1',
    volunteerCount: 10,
    attendedCount: 8,
    notAttendedCount: 2,
    averageFeedbackScore: 4.2,
    ...overrides,
  };
}
