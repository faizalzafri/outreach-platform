/**
 * API request/response types for the Outreach FMS SPA.
 */

/**
 * Normalized error structure produced by the HTTP client interceptors.
 * All API errors are transformed into this shape before propagation.
 */
export interface NormalizedError {
  status: number;
  type: string;
  message: string;
  correlationId: string | null;
  fieldErrors: Array<{ field: string; message: string }>;
}

/**
 * Standard paginated response envelope returned by Spring Boot services.
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

/**
 * Standard list query parameters shared across domain endpoints.
 */
export interface ListParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
}

/**
 * Event-specific list query parameters.
 */
export interface EventListParams extends ListParams {
  status?: import('./domain').EventStatus;
  city?: string;
  pocId?: string;
}

/**
 * Volunteer-specific list query parameters.
 */
export interface VolunteerListParams extends ListParams {
  department?: string;
  location?: string;
  availability?: import('./domain').VolunteerAvailability;
}

/**
 * Feedback-specific list query parameters.
 */
export interface FeedbackListParams extends ListParams {
  eventId?: string;
  category?: string;
  minScore?: number;
  maxScore?: number;
}

/**
 * Dashboard report parameters.
 */
export interface DashboardParams {
  startDate?: string;
  endDate?: string;
  granularity?: 'DAY' | 'WEEK' | 'MONTH' | 'QUARTER';
}

/**
 * Trend report parameters.
 */
export interface TrendParams {
  startDate: string;
  endDate: string;
  granularity: 'DAY' | 'WEEK' | 'MONTH' | 'QUARTER';
  eventIds?: string[];
  cities?: string[];
}

/**
 * Time-series report parameters.
 */
export interface TimeSeriesParams {
  startDate: string;
  endDate: string;
  granularity: 'DAY' | 'WEEK' | 'MONTH' | 'QUARTER';
  metric: string;
}

/**
 * Notification delivery query parameters.
 */
export interface DeliveryParams extends ListParams {
  status?: import('./domain').DeliveryStatus;
  eventId?: string;
}

/**
 * User administration list parameters.
 */
export interface UserListParams extends ListParams {
  role?: import('./domain').UserRole;
  status?: import('./domain').UserStatus;
}

/**
 * Audit log query parameters.
 */
export interface AuditLogParams extends ListParams {
  startDate?: string;
  endDate?: string;
  user?: string;
  action?: string;
  resourceType?: string;
}

/**
 * Backend error response structure matching the Spring Boot format.
 */
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  correlationId: string;
  fieldErrors: Array<{ field: string; message: string }>;
}

/**
 * Toast payload accepted by the toast system (or placeholder callback).
 */
export interface ToastPayload {
  severity: 'success' | 'error' | 'warning' | 'info';
  message: string;
  correlationId?: string;
}
