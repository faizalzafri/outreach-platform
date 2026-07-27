import type { Event } from '@/types/domain';

let counter = 0;

export function buildEvent(overrides: Partial<Event> = {}): Event {
  counter += 1;
  return {
    id: `evt-${counter}`,
    eventCode: `EVT-${String(counter).padStart(4, '0')}`,
    eventName: `Test Event ${counter}`,
    description: `Description for test event ${counter}`,
    status: 'DRAFT',
    eventDate: '2025-03-01',
    eventEndDate: '2025-03-01',
    city: 'Bangalore',
    venue: 'Main Hall',
    category: 'Community Service',
    maxVolunteers: 50,
    registeredCount: 10,
    attendedCount: 8,
    createdAt: '2025-02-15T10:00:00Z',
    updatedAt: '2025-02-15T10:00:00Z',
    createdBy: 'admin',
    ...overrides,
  };
}
