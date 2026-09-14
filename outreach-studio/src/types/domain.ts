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

export type VolunteerAvailability = 'AVAILABLE' | 'UNAVAILABLE' | 'ON_LEAVE';

export interface Volunteer {
  employeeId: string;
  name: string;
  email: string;
  department: string;
  location: string;
  skills: string[];
  joinDate: string;
  availability: VolunteerAvailability;
  totalEvents: number;
  averageScore: number | null;
}

export interface FeedbackSubmission {
  id: string;
  eventId: string;
  employeeId: string;
  emojiScore: number;
  textAnswer1: string;
  textAnswer2: string;
  textAnswer3?: string;
  category: string;
  anonymous: boolean;
  submittedAt: string;
}

export type ImportJobStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'COMPLETED_WITH_ERRORS' | 'FAILED';

export interface ImportJob {
  id: string;
  filename: string;
  status: ImportJobStatus;
  progress: number;
  totalRows: number;
  errorCount: number;
  createdAt: string;
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
