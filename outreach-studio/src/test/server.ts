import { setupServer } from 'msw/node'
import { handlers } from './handlers'

// Export the MSW server instance with default handlers for all domains.
// Error handlers can be added per-test via `server.use(...)`.
export const server = setupServer(...handlers)
