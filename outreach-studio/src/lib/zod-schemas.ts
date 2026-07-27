/**
 * Shared Zod Validation Schemas
 *
 * Centralized schemas for route search param validation, form validation,
 * and API request/response validation. Used across route files and form components.
 *
 * Route search param schemas use .catch() to gracefully handle invalid input —
 * invalid params are discarded and defaults applied rather than throwing errors.
 */

import { z } from 'zod';

// ─── Route Search Param Schemas ──────────────────────────────────────────────

/**
 * Event list search params — validates pagination, sorting, status filter, and text search.
 * Invalid params are discarded and defaults applied via .catch().
 */
export const eventListSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  sort: z.string().optional().catch(undefined),
  status: z
    .enum(['DRAFT', 'PUBLISHED', 'ACTIVE', 'COMPLETED', 'ARCHIVED', 'CANCELLED'])
    .optional()
    .catch(undefined),
  search: z.string().optional().catch(undefined),
});

export type EventListSearch = z.infer<typeof eventListSearchSchema>;

/**
 * Event detail search params — tab selection for the detail view.
 */
export const eventDetailSearchSchema = z.object({
  tab: z
    .enum(['overview', 'volunteers', 'feedback', 'notifications', 'audit'])
    .default('overview')
    .catch('overview'),
});

export type EventDetailSearch = z.infer<typeof eventDetailSearchSchema>;

/**
 * Volunteer list search params — pagination and text search.
 */
export const volunteerListSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  sort: z.string().optional().catch(undefined),
  search: z.string().optional().catch(undefined),
});

export type VolunteerListSearch = z.infer<typeof volunteerListSearchSchema>;

/**
 * Feedback list search params — pagination and event filter.
 */
export const feedbackSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  eventId: z.string().optional().catch(undefined),
});

export type FeedbackSearch = z.infer<typeof feedbackSearchSchema>;

/**
 * Notification list search params — pagination and type filter.
 */
export const notificationSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  type: z.enum(['EMAIL', 'SMS', 'PUSH']).optional().catch(undefined),
});

export type NotificationSearch = z.infer<typeof notificationSearchSchema>;

/**
 * Reports search params — date range, granularity, and tab selection.
 */
export const reportSearchSchema = z.object({
  startDate: z.string().optional().catch(undefined),
  endDate: z.string().optional().catch(undefined),
  granularity: z.enum(['DAY', 'WEEK', 'MONTH', 'QUARTER']).default('DAY').catch('DAY'),
  tab: z.enum(['event', 'beneficiary', 'city', 'poc']).default('event').catch('event'),
});

export type ReportSearch = z.infer<typeof reportSearchSchema>;

/**
 * Admin user list search params — pagination, role, and status filters.
 */
export const adminSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  role: z.enum(['ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC']).optional().catch(undefined),
  status: z.enum(['ENABLED', 'DISABLED', 'LOCKED']).optional().catch(undefined),
});

export type AdminSearch = z.infer<typeof adminSearchSchema>;

/**
 * Audit log search params — pagination, user, action, resource type, date range.
 */
export const auditLogSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(50).catch(50),
  user: z.string().optional().catch(undefined),
  action: z.string().optional().catch(undefined),
  resourceType: z.string().optional().catch(undefined),
  startDate: z.string().optional().catch(undefined),
  endDate: z.string().optional().catch(undefined),
});

export type AuditLogSearch = z.infer<typeof auditLogSearchSchema>;

/**
 * Ingestion job list search params.
 */
export const ingestionSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  status: z
    .enum(['PENDING', 'IN_PROGRESS', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED'])
    .optional()
    .catch(undefined),
});

export type IngestionSearch = z.infer<typeof ingestionSearchSchema>;

// ─── Form Validation Schemas ─────────────────────────────────────────────────

/**
 * Event creation/edit form schema.
 */
export const eventCreateSchema = z
  .object({
    name: z.string().min(1).max(100),
    code: z.string().min(1).max(20).regex(/^[a-zA-Z0-9]+$/),
    description: z.string().min(1).max(2000),
    startDate: z.string().datetime(),
    endDate: z.string().datetime(),
    city: z.string().min(1),
    venue: z.string().min(1),
    category: z.string().min(1),
    maxVolunteers: z.number().int().min(1).max(10000),
  })
  .refine((data) => new Date(data.endDate) >= new Date(data.startDate), {
    message: 'End date must be on or after start date',
    path: ['endDate'],
  });

export type EventCreateForm = z.infer<typeof eventCreateSchema>;

/**
 * Feedback submission form schema.
 */
export const feedbackFormSchema = z.object({
  emojiScore: z.number().int().min(1).max(5),
  textAnswer1: z.string().min(1).max(500),
  textAnswer2: z.string().min(1).max(500),
  textAnswer3: z.string().max(500).optional(),
  category: z.string().min(1),
  anonymous: z.boolean().default(false),
});

export type FeedbackForm = z.infer<typeof feedbackFormSchema>;

/**
 * User creation form schema.
 */
export const userCreateSchema = z.object({
  username: z.string().min(3).max(50).regex(/^[a-zA-Z0-9_]+$/),
  email: z.string().email().max(254),
  role: z.enum(['ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC']),
});

export type UserCreateForm = z.infer<typeof userCreateSchema>;

/**
 * Cron expression validation (5-field format).
 */
export const cronSchema = z.string().regex(
  /^(\*|[0-9,\-/]+)\s+(\*|[0-9,\-/]+)\s+(\*|[0-9,\-/]+)\s+(\*|[0-9,\-/]+)\s+(\*|[0-9,\-/]+)$/,
  'Invalid cron expression'
);
