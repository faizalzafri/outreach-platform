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

export type DeliveryStatus = 'PENDING' | 'SENT' | 'DELIVERED' | 'FAILED' | 'BOUNCED';

export interface DeliveryRecord {
  id: string;
  recipient: string;
  eventName: string;
  status: DeliveryStatus;
  timestamp: string;
}

export interface AuditEntry {
  id: string;
  timestamp: string;
  user: string;
  action: string;
  resourceType: string;
  resourceId: string;
  ipAddress: string;
  payload: Record<string, unknown>;
}

export type UserRole = 'ROLE_ADMIN' | 'ROLE_PMO' | 'ROLE_POC';
export type UserStatus = 'ENABLED' | 'DISABLED' | 'LOCKED';

export interface User {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  enabled: boolean;
}

export interface DashboardKPIs {
  totalEvents: number;
  activeEvents: number;
  totalVolunteers: number;
  averageFeedbackScore: number;
  pendingFeedback: number;
  notificationDeliveryRate: number;
}
