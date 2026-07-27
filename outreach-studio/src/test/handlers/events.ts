import { http, HttpResponse } from 'msw'

export const eventHandlers = [
  // GET /api/events - paginated list
  http.get('/api/events', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '10')

    return HttpResponse.json({
      content: [
        {
          id: 'evt-001',
          code: 'EVT2024001',
          name: 'Annual Volunteer Drive',
          status: 'PUBLISHED',
          startDate: '2024-06-01T09:00:00Z',
          endDate: '2024-06-03T17:00:00Z',
          city: 'Mumbai',
          venue: 'Convention Center',
          category: 'CSR',
          maxVolunteers: 200,
          volunteerCount: 45,
          pocId: 'user-001',
          pocName: 'Priya Sharma',
          createdAt: '2024-05-01T10:00:00Z',
          updatedAt: '2024-05-15T14:30:00Z',
        },
        {
          id: 'evt-002',
          code: 'EVT2024002',
          name: 'Tech for Good Hackathon',
          status: 'DRAFT',
          startDate: '2024-07-15T08:00:00Z',
          endDate: '2024-07-16T18:00:00Z',
          city: 'Bangalore',
          venue: 'Tech Park Auditorium',
          category: 'TECH',
          maxVolunteers: 100,
          volunteerCount: 0,
          pocId: 'user-002',
          pocName: 'Rahul Verma',
          createdAt: '2024-05-10T08:00:00Z',
          updatedAt: '2024-05-10T08:00:00Z',
        },
      ],
      totalElements: 25,
      totalPages: Math.ceil(25 / size),
      page,
      size,
    })
  }),

  // POST /api/events - create event
  http.post('/api/events', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json(
      {
        id: 'evt-new-001',
        ...body,
        status: 'DRAFT',
        volunteerCount: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      { status: 201 },
    )
  }),

  // GET /api/events/:eventId - event detail
  http.get('/api/events/:eventId', ({ params }) => {
    return HttpResponse.json({
      id: params.eventId,
      code: 'EVT2024001',
      name: 'Annual Volunteer Drive',
      description: 'A large-scale volunteer initiative across multiple cities.',
      status: 'PUBLISHED',
      startDate: '2024-06-01T09:00:00Z',
      endDate: '2024-06-03T17:00:00Z',
      city: 'Mumbai',
      venue: 'Convention Center',
      category: 'CSR',
      maxVolunteers: 200,
      volunteerCount: 45,
      attendance: 38,
      avgFeedbackScore: 4.2,
      pocId: 'user-001',
      pocName: 'Priya Sharma',
      createdAt: '2024-05-01T10:00:00Z',
      updatedAt: '2024-05-15T14:30:00Z',
    })
  }),

  // PATCH /api/events/:eventId/status - lifecycle transition
  http.patch('/api/events/:eventId/status', async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json({
      id: params.eventId,
      status: body.status,
      updatedAt: new Date().toISOString(),
    })
  }),

  // POST /api/events/:eventId/volunteers - enroll volunteer
  http.post('/api/events/:eventId/volunteers', async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json(
      {
        eventId: params.eventId,
        employeeId: body.employeeId,
        enrolledAt: new Date().toISOString(),
        status: 'ENROLLED',
      },
      { status: 201 },
    )
  }),
]

export const eventErrorHandlers = {
  badRequest: http.post('/api/events', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Validation failed',
        details: [
          { field: 'name', message: 'Name is required' },
          { field: 'startDate', message: 'Start date must be in the future' },
        ],
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  unauthorized: http.get('/api/events', () => {
    return HttpResponse.json(
      { error: 'Unauthorized', message: 'Token expired or invalid' },
      {
        status: 401,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  serverError: http.get('/api/events', () => {
    return HttpResponse.json(
      { error: 'Internal Server Error', message: 'An unexpected error occurred' },
      {
        status: 500,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  invalidTransition: http.patch('/api/events/:eventId/status', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Invalid status transition',
        allowedTransitions: ['ACTIVATE', 'CANCEL'],
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  duplicateEnrollment: http.post('/api/events/:eventId/volunteers', () => {
    return HttpResponse.json(
      { error: 'Conflict', message: 'Volunteer already enrolled in this event' },
      {
        status: 409,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),
}
