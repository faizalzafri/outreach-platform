// Domain entity types for the Outreach Feedback Management System

export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'ACTIVE' | 'COMPLETED' | 'ARCHIVED' | 'CANCELLED';

/** Valid lifecycle transitions for each event status. */
export const EVENT_TRANSITIONS: Record<EventStatus, EventStatus[]> = {
  DRAFT: ['PUBLISHED', 'CANCELLED'],
  PUBLISHED: ['ACTIVE', 'CANCELLED'],
  ACTIVE: ['COMPLETED'],
  COMPLETED: ['ARCHIVED'],
  ARCHIVED: [],
  CANCELLED: [],
};

export interface Event {
  id: string;
  eventCode: string;
  eventName: string;
  description: string;
  status: EventStatus;
  eventDate: string;
  eventEndDate: string;
  city: string;
  venue: string;
  category: string;
  maxVolunteers: number;
  registeredCount: number;
  attendedCount: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export type VolunteerAvailability = 'AVAILABLE' | 'BUSY' | 'ON_LEAVE';

export interface Volunteer {
  id: string;
  employeeId: string;
  fullName: string;
  email: string;
  phone: string;
  baseLocation: string;
  department: string;
  designation: string;
  skills: string;
  availability: VolunteerAvailability;
  totalEventsParticipated: number;
  avgFeedbackScore: number | null;
}

export interface VolunteerHistoryEntry {
  eventId: string;
  eventName: string;
  eventCode: string;
  eventDate: string;
  city: string;
  attendanceStatus: AttendanceStatus;
  registeredAt: string;
}

export type FeedbackSentiment = 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';
export type FeedbackStatus = 'SUBMITTED' | 'REVIEWED' | 'FLAGGED' | 'ARCHIVED';

export interface FeedbackSubmission {
  id: string;
  eventId: string;
  volunteerId: string;
  score: number;
  answer1: string;
  answer2: string;
  answer3?: string;
  category: string;
  tags?: string;
  sentiment?: FeedbackSentiment;
  status: FeedbackStatus;
  anonymous: boolean;
  submittedAt: string;
  /** Filled in by feedback-service from event-service on lists; null for anonymous feedback. */
  eventName?: string | null;
  volunteerName?: string | null;
}

export type AttendanceStatus = 'REGISTERED' | 'ATTENDED' | 'NOT_ATTENDED' | 'UNREGISTERED';
export type EmailStatus = 'PENDING' | 'SENT' | 'DELIVERED' | 'FAILED' | 'BOUNCED';

/** A volunteer's enrollment record for a specific event (`GET /events/{eventId}/volunteers`). */
export interface EventEnrollment {
  id: string;
  eventId: string;
  volunteerId: string;
  employeeId: string;
  volunteerName: string;
  attendanceStatus: AttendanceStatus;
  emailStatus: EmailStatus;
  registeredAt: string;
  attendanceMarkedAt?: string;
}

/** An organisation an event serves. */
export interface Beneficiary {
  id: string;
  name: string;
  organization: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  city: string | null;
  address: string | null;
  description: string | null;
  active: boolean;
}

export type AssignmentRole = 'PRIMARY' | 'SECONDARY';

/** A user assigned to an event as one of its points of contact. */
export interface PocAssignment {
  id: string;
  eventId: string;
  eventName: string;
  userId: string;
  username: string;
  assignmentRole: AssignmentRole;
  assignedAt: string;
  assignedBy: string | null;
}

export type ImportJobStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface ImportJob {
  id: string;
  fileName: string;
  status: ImportJobStatus;
  progress: number;
  totalRows: number;
  errorCount: number;
  createdAt: string;
}

/** Matches ingestion-service's ValidationError record. */
export interface JobError {
  rowNumber: number;
  columnName: string;
  errorMessage: string;
  rejectedValue: string | null;
}

export type NotificationType = 'EMAIL' | 'SMS' | 'PUSH';

export interface NotificationTemplate {
  id: string;
  name: string;
  type: NotificationType;
  subjectTemplate: string | null;
  bodyTemplate: string;
  engine: string;
  active: boolean;
  version: number;
  variablesSchema: string | null;
}

export type DeliveryStatus =
  | 'PENDING' | 'QUEUED' | 'SENT' | 'DELIVERED' | 'FAILED' | 'PERMANENTLY_FAILED' | 'BOUNCED';

/** One email as tracked by notification-service (GET /notifications/history). */
export interface DeliveryRecord {
  id: string;
  eventId: string;
  recipientEmail: string;
  subject: string;
  status: DeliveryStatus;
  createdAt: string;
  sentAt: string | null;
  lastAttemptAt: string | null;
}

export interface AuditEntry {
  id: string;
  timestamp: string;
  userId: string;
  action: string;
  resourceType: string;
  resourceId: string;
  details: Record<string, unknown>;
}

export type UserRole = 'ROLE_ADMIN' | 'ROLE_PMO' | 'ROLE_POC';
export type UserStatus = 'ENABLED' | 'DISABLED' | 'LOCKED';

/** A user in event-service's directory (read-only; used for pickers such as POC assignment). */
export interface User {
  id: string;
  username: string;
  displayName: string | null;
  email: string;
  role: UserRole;
  enabled: boolean;
}

/** Tenant role as auth-service names it (no ROLE_ prefix). */
export type AccountRole = 'ADMIN' | 'PMO' | 'POC';
export type AccountStatus = 'INVITED' | 'ACTIVE' | 'DISABLED';

/** A sign-in account as returned by auth-service's user administration API (/api/auth/users). */
export interface Account {
  id: string;
  username: string;
  displayName: string;
  email: string;
  phone: string | null;
  role: AccountRole;
  status: AccountStatus;
  locked: boolean;
  lastLoginAt: string | null;
  createdAt: string | null;
}

export type OtpPurpose = 'LOGIN' | 'PASSWORD_RESET' | 'PASSWORD_CHANGE';

export interface OtpSettings {
  enabled: boolean;
  length: number;
  ttlSeconds: number;
  maxAttempts: number;
  resendCooldownSeconds: number;
}

/** An organization's sign-in security (/api/auth/security-policy). */
export interface SecurityPolicy {
  passwordMinLength: number;
  passwordHistoryCount: number;
  /** Read-only: the platform floor the organization cannot go below. */
  platformMinLength?: number;
  platformHistoryCount?: number;
  otp: Record<OtpPurpose, OtpSettings>;
}

/** The signed-in user's own account (/api/auth/me). */
export interface Profile {
  id: string;
  username: string;
  displayName: string;
  email: string;
  phone: string | null;
  platformAdmin: boolean;
  lastLoginAt: string | null;
  passwordChangedAt: string | null;
  passwordMinLength: number;
}

/** Dashboard tiles, combined from GET /reports/dashboard and GET /reports/dashboard/kpis. */
export interface DashboardKPIs {
  totalEvents: number;
  completedEvents: number;
  totalVolunteers: number;
  averageFeedbackScore: number;
  totalFeedbackSubmissions: number;
  feedbackCompletionRate: number;
}
