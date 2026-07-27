import type { AuditEntry } from '@/types/domain';

let counter = 0;

export function buildAuditEntry(overrides: Partial<AuditEntry> = {}): AuditEntry {
  counter += 1;
  return {
    id: `audit-${counter}`,
    timestamp: '2025-03-01T14:30:00Z',
    user: `user-${counter}`,
    action: 'CREATE',
    resourceType: 'Event',
    resourceId: `evt-${counter}`,
    ipAddress: '192.168.1.1',
    payload: {},
    ...overrides,
  };
}
