import type { DashboardKPIs } from '@/types/domain';

export function buildDashboardKPIs(overrides: Partial<DashboardKPIs> = {}): DashboardKPIs {
  return {
    totalEvents: 25,
    completedEvents: 5,
    totalVolunteers: 200,
    averageFeedbackScore: 4.3,
    totalFeedbackSubmissions: 12,
    feedbackCompletionRate: 48.5,
    ...overrides,
  };
}
