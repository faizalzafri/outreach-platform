import { http, HttpResponse } from 'msw'

export const volunteerHandlers = [
  // GET /api/events/volunteers - paginated list
  http.get('/api/events/volunteers', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '10')

    return HttpResponse.json({
      content: [
        {
          employeeId: 'EMP001',
          name: 'Anita Desai',
          email: 'anita.desai@company.com',
          department: 'Engineering',
          location: 'Mumbai',
          skills: ['JavaScript', 'React', 'Node.js'],
          totalEvents: 12,
          avgScore: 4.5,
          availability: 'AVAILABLE',
          joinDate: '2022-03-15T00:00:00Z',
        },
        {
          employeeId: 'EMP002',
          name: 'Vikram Singh',
          email: 'vikram.singh@company.com',
          department: 'Marketing',
          location: 'Delhi',
          skills: ['Content Writing', 'Social Media', 'Event Planning'],
          totalEvents: 8,
          avgScore: 4.1,
          availability: 'UNAVAILABLE',
          joinDate: '2023-01-10T00:00:00Z',
        },
      ],
      totalElements: 150,
      totalPages: Math.ceil(150 / size),
      page,
      size,
    })
  }),

  // GET /api/events/volunteers/:employeeId - volunteer detail
  http.get('/api/events/volunteers/:employeeId', ({ params }) => {
    return HttpResponse.json({
      employeeId: params.employeeId,
      name: 'Anita Desai',
      email: 'anita.desai@company.com',
      department: 'Engineering',
      location: 'Mumbai',
      skills: ['JavaScript', 'React', 'Node.js'],
      totalEvents: 12,
      avgScore: 4.5,
      availability: 'AVAILABLE',
      joinDate: '2022-03-15T00:00:00Z',
      participationHistory: [
        {
          eventId: 'evt-001',
          eventName: 'Annual Volunteer Drive',
          date: '2024-06-01T09:00:00Z',
          role: 'PARTICIPANT',
          feedbackScore: 5,
        },
        {
          eventId: 'evt-003',
          eventName: 'Beach Cleanup',
          date: '2024-04-22T07:00:00Z',
          role: 'LEAD',
          feedbackScore: 4,
        },
      ],
    })
  }),

  // PUT /api/events/volunteers/:employeeId/availability - update availability
  http.put('/api/events/volunteers/:employeeId/availability', async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json({
      employeeId: params.employeeId,
      availability: body.availability,
      updatedAt: new Date().toISOString(),
    })
  }),
]

export const volunteerErrorHandlers = {
  badRequest: http.put('/api/events/volunteers/:employeeId/availability', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Invalid availability value',
        details: [{ field: 'availability', message: 'Must be AVAILABLE or UNAVAILABLE' }],
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  unauthorized: http.get('/api/events/volunteers', () => {
    return HttpResponse.json(
      { error: 'Unauthorized', message: 'Token expired or invalid' },
      {
        status: 401,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  serverError: http.get('/api/events/volunteers', () => {
    return HttpResponse.json(
      { error: 'Internal Server Error', message: 'An unexpected error occurred' },
      {
        status: 500,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  notFound: http.get('/api/events/volunteers/:employeeId', () => {
    return HttpResponse.json(
      { error: 'Not Found', message: 'Volunteer not found' },
      {
        status: 404,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),
}
