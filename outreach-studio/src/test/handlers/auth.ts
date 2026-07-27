import { http, HttpResponse } from 'msw'

const REALM = 'outreach'

export const authHandlers = [
  // POST /auth/realms/:realm/protocol/openid-connect/token - token exchange
  http.post('/auth/realms/:realm/protocol/openid-connect/token', () => {
    return HttpResponse.json({
      access_token: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTAwMSIsImVtYWlsIjoicHJpeWEuc2hhcm1hQGNvbXBhbnkuY29tIiwibmFtZSI6IlByaXlhIFNoYXJtYSIsInByZWZlcnJlZF91c2VybmFtZSI6InByaXlhX3NoYXJtYSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJST0xFX1BNTyJdfSwiZXhwIjo5OTk5OTk5OTk5LCJpYXQiOjE3MTcwMDAwMDB9.mock-signature',
      refresh_token: 'mock-refresh-token-abc123',
      token_type: 'Bearer',
      expires_in: 300,
      refresh_expires_in: 1800,
      scope: 'openid profile email',
    })
  }),

  // POST /auth/realms/:realm/protocol/openid-connect/revoke - token revocation
  http.post('/auth/realms/:realm/protocol/openid-connect/revoke', () => {
    return new HttpResponse(null, { status: 204 })
  }),
]

export const authErrorHandlers = {
  badRequest: http.post('/auth/realms/:realm/protocol/openid-connect/token', () => {
    return HttpResponse.json(
      {
        error: 'invalid_grant',
        error_description: 'Code not valid',
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  unauthorized: http.post('/auth/realms/:realm/protocol/openid-connect/token', () => {
    return HttpResponse.json(
      {
        error: 'invalid_client',
        error_description: 'Invalid client credentials',
      },
      {
        status: 401,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  serverError: http.post('/auth/realms/:realm/protocol/openid-connect/token', () => {
    return HttpResponse.json(
      {
        error: 'server_error',
        error_description: 'Authentication service unavailable',
      },
      {
        status: 500,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  tokenExpired: http.post('/auth/realms/:realm/protocol/openid-connect/token', () => {
    return HttpResponse.json(
      {
        error: 'invalid_grant',
        error_description: 'Token is not active',
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),
}

// Export the realm constant for use in tests
export const TEST_REALM = REALM
