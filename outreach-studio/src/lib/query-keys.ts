import type {
  EventListParams,
  VolunteerListParams,
  FeedbackListParams,
  DashboardParams,
  TrendParams,
  TimeSeriesParams,
  DeliveryParams,
  UserListParams,
  AuditLogParams,
} from '@/types/api';

/**
 * Structured query key factory for all domains.
 *
 * Using a factory pattern ensures consistent, hierarchical cache keys that
 * enable granular invalidation (e.g., invalidate all event lists without
 * touching event details).
 */
export const queryKeys = {
  events: {
    all: ['events'] as const,
    lists: () => [...queryKeys.events.all, 'list'] as const,
    list: (params: EventListParams) => [...queryKeys.events.lists(), params] as const,
    details: () => [...queryKeys.events.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.events.details(), id] as const,
  },
  volunteers: {
    all: ['volunteers'] as const,
    lists: () => [...queryKeys.volunteers.all, 'list'] as const,
    list: (params: VolunteerListParams) => [...queryKeys.volunteers.lists(), params] as const,
    details: () => [...queryKeys.volunteers.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.volunteers.details(), id] as const,
  },
  feedback: {
    all: ['feedback'] as const,
    lists: () => [...queryKeys.feedback.all, 'list'] as const,
    list: (params: FeedbackListParams) => [...queryKeys.feedback.lists(), params] as const,
  },
  reports: {
    dashboard: (params: DashboardParams) => ['reports', 'dashboard', params] as const,
    trends: (params: TrendParams) => ['reports', 'trends', params] as const,
    timeSeries: (params: TimeSeriesParams) => ['reports', 'time-series', params] as const,
    export: (jobId: string) => ['reports', 'export', jobId] as const,
  },
  ingestion: {
    all: ['ingestion'] as const,
    jobs: () => [...queryKeys.ingestion.all, 'jobs'] as const,
    job: (id: string) => [...queryKeys.ingestion.all, 'jobs', id] as const,
    jobErrors: (id: string) => [...queryKeys.ingestion.all, 'jobs', id, 'errors'] as const,
  },
  notifications: {
    all: ['notifications'] as const,
    templates: () => [...queryKeys.notifications.all, 'templates'] as const,
    deliveries: (params: DeliveryParams) => [...queryKeys.notifications.all, 'deliveries', params] as const,
  },
  admin: {
    all: ['admin'] as const,
    users: (params?: UserListParams) => [...queryKeys.admin.all, 'users', params] as const,
    auditLog: (params: AuditLogParams) => [...queryKeys.admin.all, 'audit-log', params] as const,
  },
} as const;
