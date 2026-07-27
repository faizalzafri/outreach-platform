import { http, HttpResponse } from 'msw'

export const auditLogHandlers = [
  // GET /api/admin/audit-log - paginated list
  http.get('/api/admin/audit-log', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '50')

    return HttpResponse.json({
      content: [
        {
          id: 'audit-001',
          timestamp: '2024-06-02T15:30:00Z',
          userId: 'user-001',
          username: 'priya_sharma',
          action: 'EVENT_CREATED',
          resourceType: 'EVENT',
          resourceId: 'evt-001',
          ipAddress: '192.168.1.100',
          payload: {
            eventName: 'Annual Volunteer Drive',
            eventCode: 'EVT2024001',
          },
        },
        {
          id: 'audit-002',
          timestamp: '2024-06-02T14:00:00Z',
          userId: 'user-003',
          username: 'admin_user',
          action: 'USER_ROLE_CHANGED',
          resourceType: 'USER',
          resourceId: 'user-002',
          ipAddress: '10.0.0.50',
          payload: {
            previousRole: 'ROLE_POC',
            newRole: 'ROLE_PMO',
          },
        },
        {
          id: 'audit-003',
          timestamp: '2024-06-02T12:15:00Z',
          userId: 'user-002',
          username: 'rahul_verma',
          action: 'VOLUNTEER_ENROLLED',
          resourceType: 'VOLUNTEER',
          resourceId: 'EMP005',
          ipAddress: '172.16.0.25',
          payload: {
            eventId: 'evt-001',
            employeeId: 'EMP005',
          },
        },
      ],
      totalElements: 1250,
      totalPages: Math.ceil(1250 / size),
      page,
      size,
      cursor: 'eyJ0aW1lc3RhbXAiOiIyMDI0LTA2LTAyVDEyOjE1OjAwWiJ9',
    })
  }),

  // GET /api/admin/audit-log/export - CSV export
  http.get('/api/admin/audit-log/export', () => {
    return new HttpResponse(
      'timestamp,user,action,resourceType,resourceId,ipAddress\n2024-06-02T15:30:00Z,priya_sharma,EVENT_CREATED,EVENT,evt-001,192.168.1.100\n2024-06-02T14:00:00Z,admin_user,USER_ROLE_CHANGED,USER,user-002,10.0.0.50\n',
      {
        status: 200,
        headers: {
          'Content-Type': 'text/csv',
          'Content-Disposition': 'attachment; filename="audit-log-export.csv"',
        },
      },
    )
  }),
]

export const auditLogErrorHandlers = {
  badRequest: http.get('/api/admin/audit-log', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Invalid filter parameters',
        details: [{ field: 'startDate', message: 'Start date must be before end date' }],
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  unauthorized: http.get('/api/admin/audit-log', () => {
    return HttpResponse.json(
      { error: 'Unauthorized', message: 'Token expired or invalid' },
      {
        status: 401,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  serverError: http.get('/api/admin/audit-log', () => {
    return HttpResponse.json(
      { error: 'Internal Server Error', message: 'An unexpected error occurred' },
      {
        status: 500,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  exportError: http.get('/api/admin/audit-log/export', () => {
    return HttpResponse.json(
      {
        error: 'Internal Server Error',
        message: 'Export generation failed',
      },
      {
        status: 500,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),
}
