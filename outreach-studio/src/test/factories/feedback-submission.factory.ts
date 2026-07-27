import type { FeedbackSubmission } from '@/types/domain';

let counter = 0;

export function buildFeedbackSubmission(overrides: Partial<FeedbackSubmission> = {}): FeedbackSubmission {
  counter += 1;
  return {
    id: `fb-${counter}`,
    eventId: `evt-${counter}`,
    employeeId: `emp-${counter}`,
    emojiScore: 4,
    textAnswer1: 'Great experience overall',
    textAnswer2: 'Well organized event',
    category: 'General',
    anonymous: false,
    submittedAt: '2025-03-02T10:30:00Z',
    ...overrides,
  };
}
