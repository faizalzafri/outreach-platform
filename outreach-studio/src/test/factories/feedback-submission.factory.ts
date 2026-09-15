import type { FeedbackSubmission } from '@/types/domain';

let counter = 0;

export function buildFeedbackSubmission(overrides: Partial<FeedbackSubmission> = {}): FeedbackSubmission {
  counter += 1;
  return {
    id: `fb-${counter}`,
    eventId: `evt-${counter}`,
    volunteerId: `vol-${counter}`,
    score: 4,
    answer1: 'Great experience overall',
    answer2: 'Well organized event',
    category: 'General',
    status: 'SUBMITTED',
    anonymous: false,
    submittedAt: '2025-03-02T10:30:00Z',
    ...overrides,
  };
}
