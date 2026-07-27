import type { DashboardKPIs } from '@/types/domain';

export function buildDashboardKPIs(overrides: Partial<DashboardKPIs> = {}): DashboardKPIs {
  return {
    totalEvents: 25,
    activeEvents: 5,
    totalVolunteers: 200,
    averageFeedbackScore: 4.3,
    pendingFeedback: 12,
    notificationDeliveryRate: 0.95,
    ...overrides,
  };
}
