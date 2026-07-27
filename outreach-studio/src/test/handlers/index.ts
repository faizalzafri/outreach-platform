import { eventHandlers } from './events'
import { volunteerHandlers } from './volunteers'
import { feedbackHandlers } from './feedback'
import { ingestionHandlers } from './ingestion'
import { notificationHandlers } from './notifications'
import { reportHandlers } from './reports'
import { adminHandlers } from './admin'
import { auditLogHandlers } from './audit-log'
import { authHandlers } from './auth'

/**
 * Combined default handlers for all domains.
 * Use with `server.use(...handlers)` or pass to `setupServer(...handlers)`.
 * Individual error handlers are exported from each domain module for per-test use.
 */
export const handlers = [
  ...eventHandlers,
  ...volunteerHandlers,
  ...feedbackHandlers,
  ...ingestionHandlers,
  ...notificationHandlers,
  ...reportHandlers,
  ...adminHandlers,
  ...auditLogHandlers,
  ...authHandlers,
]

// Re-export individual handler arrays for selective use in tests
export { eventHandlers, eventErrorHandlers } from './events'
export { volunteerHandlers, volunteerErrorHandlers } from './volunteers'
export { feedbackHandlers, feedbackErrorHandlers } from './feedback'
export { ingestionHandlers, ingestionErrorHandlers } from './ingestion'
export { notificationHandlers, notificationErrorHandlers } from './notifications'
export { reportHandlers, reportErrorHandlers } from './reports'
export { adminHandlers, adminErrorHandlers } from './admin'
export { auditLogHandlers, auditLogErrorHandlers } from './audit-log'
export { authHandlers, authErrorHandlers, TEST_REALM } from './auth'
