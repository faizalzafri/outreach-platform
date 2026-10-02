import type { DeliveryRecord } from '@/types/domain';

let counter = 0;

export function buildDeliveryRecord(overrides: Partial<DeliveryRecord> = {}): DeliveryRecord {
  counter += 1;
  return {
    id: `dlv-${counter}`,
    eventId: `evt-${counter}`,
    recipientEmail: `volunteer${counter}@example.com`,
    subject: `Event ${counter}`,
    status: 'DELIVERED',
    createdAt: '2025-03-01T12:00:00Z',
    sentAt: '2025-03-01T12:00:05Z',
    lastAttemptAt: '2025-03-01T12:00:05Z',
    ...overrides,
  };
}
