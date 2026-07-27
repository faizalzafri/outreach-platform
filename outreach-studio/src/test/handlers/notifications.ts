import { http, HttpResponse } from 'msw'

export const notificationHandlers = [
  // GET /api/notifications/templates - list templates
  http.get('/api/notifications/templates', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '10')

    return HttpResponse.json({
      content: [
        {
          id: 'tpl-001',
          name: 'Event Registration Confirmation',
          type: 'EMAIL',
          subject: 'You are registered for {{eventName}}',
          engine: 'HANDLEBARS',
          status: 'ACTIVE',
          version: 3,
          createdAt: '2024-01-15T10:00:00Z',
          updatedAt: '2024-05-20T16:00:00Z',
        },
        {
          id: 'tpl-002',
          name: 'Event Reminder SMS',
          type: 'SMS',
          subject: null,
          engine: 'HANDLEBARS',
          status: 'DRAFT',
          version: 1,
          createdAt: '2024-05-01T08:00:00Z',
          updatedAt: '2024-05-01T08:00:00Z',
        },
      ],
      totalElements: 18,
      totalPages: Math.ceil(18 / size),
      page,
      size,
    })
  }),

  // POST /api/notifications/templates - create template
  http.post('/api/notifications/templates', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json(
      {
        id: 'tpl-new-001',
        ...body,
        status: 'DRAFT',
        version: 1,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      { status: 201 },
    )
  }),

  // GET /api/notifications/templates/:id/preview - preview template
  http.get('/api/notifications/templates/:id/preview', () => {
    return HttpResponse.json({
      rendered: '<html><body><h1>Hello Anita!</h1><p>You are registered for Annual Volunteer Drive on June 1, 2024.</p></body></html>',
      subject: 'You are registered for Annual Volunteer Drive',
      variables: { employeeName: 'Anita', eventName: 'Annual Volunteer Drive', eventDate: 'June 1, 2024' },
    })
  }),

  // GET /api/notifications/deliveries - delivery history
  http.get('/api/notifications/deliveries', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '10')

    return HttpResponse.json({
      content: [
        {
          id: 'del-001',
          templateId: 'tpl-001',
          recipientEmail: 'anita.desai@company.com',
          recipientName: 'Anita Desai',
          eventId: 'evt-001',
          eventName: 'Annual Volunteer Drive',
          status: 'DELIVERED',
          sentAt: '2024-06-01T08:00:00Z',
          deliveredAt: '2024-06-01T08:00:05Z',
        },
        {
          id: 'del-002',
          templateId: 'tpl-001',
          recipientEmail: 'vikram.singh@company.com',
          recipientName: 'Vikram Singh',
          eventId: 'evt-001',
          eventName: 'Annual Volunteer Drive',
          status: 'FAILED',
          sentAt: '2024-06-01T08:00:00Z',
          deliveredAt: null,
          failureReason: 'Mailbox full',
        },
      ],
      totalElements: 230,
      totalPages: Math.ceil(230 / size),
      page,
      size,
    })
  }),

  // POST /api/notifications/retry/:eventId - retry failed
  http.post('/api/notifications/retry/:eventId', () => {
    return HttpResponse.json({
      retriedCount: 3,
      eventId: 'evt-001',
      status: 'RETRYING',
    })
  }),

  // POST /api/notifications/schedule - schedule notification
  http.post('/api/notifications/schedule', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json(
      {
        id: 'sched-001',
        ...body,
        status: 'SCHEDULED',
        createdAt: new Date().toISOString(),
      },
      { status: 201 },
    )
  }),
]

export const notificationErrorHandlers = {
  badRequest: http.post('/api/notifications/templates', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Validation failed',
        details: [
          { field: 'name', message: 'Template name is required' },
          { field: 'body', message: 'Template body must not exceed 10000 characters' },
        ],
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  unauthorized: http.get('/api/notifications/templates', () => {
    return HttpResponse.json(
      { error: 'Unauthorized', message: 'Token expired or invalid' },
      {
        status: 401,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  serverError: http.get('/api/notifications/deliveries', () => {
    return HttpResponse.json(
      { error: 'Internal Server Error', message: 'An unexpected error occurred' },
      {
        status: 500,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  previewFailed: http.get('/api/notifications/templates/:id/preview', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Template rendering failed: missing required variable "eventName"',
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  invalidCron: http.post('/api/notifications/schedule', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Invalid cron expression',
        details: [{ field: 'cronExpression', message: 'Must be a valid 5-field cron expression' }],
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),
}
