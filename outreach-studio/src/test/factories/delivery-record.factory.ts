import type { DeliveryRecord } from '@/types/domain';

let counter = 0;

export function buildDeliveryRecord(overrides: Partial<DeliveryRecord> = {}): DeliveryRecord {
  counter += 1;
  return {
    id: `dlv-${counter}`,
    recipient: `volunteer${counter}@example.com`,
    eventName: `Event ${counter}`,
    status: 'DELIVERED',
    timestamp: '2025-03-01T12:00:00Z',
    ...overrides,
  };
}
