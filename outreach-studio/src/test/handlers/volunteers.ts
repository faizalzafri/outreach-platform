import { http, HttpResponse } from 'msw'

export const volunteerHandlers = [
  // GET /api/volunteers - paginated list, optionally filtered by ?search=
  http.get('/api/volunteers', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '10')

    return HttpResponse.json({
      content: [
        {
          id: 'c0000000-0000-0000-0000-000000000001',
          employeeId: 'EMP001',
          fullName: 'Anita Desai',
          email: 'anita.desai@company.com',
          phone: '9876543210',
          baseLocation: 'Mumbai',
          department: 'Engineering',
          designation: 'Senior Developer',
          skills: 'JavaScript,React,Node.js',
          availability: 'AVAILABLE',
          totalEventsParticipated: 12,
          avgFeedbackScore: 4.5,
        },
        {
          id: 'c0000000-0000-0000-0000-000000000002',
          employeeId: 'EMP002',
          fullName: 'Vikram Singh',
          email: 'vikram.singh@company.com',
          phone: '9876543211',
          baseLocation: 'Delhi',
          department: 'Marketing',
          designation: 'Marketing Lead',
          skills: 'Content Writing,Social Media,Event Planning',
          availability: 'BUSY',
          totalEventsParticipated: 8,
          avgFeedbackScore: 4.1,
        },
      ],
      totalElements: 150,
      totalPages: Math.ceil(150 / size),
      page,
      size,
    })
  }),

  // GET /api/volunteers/:employeeId - volunteer detail
  http.get('/api/volunteers/:employeeId', ({ params }) => {
    return HttpResponse.json({
      id: 'c0000000-0000-0000-0000-000000000001',
      employeeId: params.employeeId,
      fullName: 'Anita Desai',
      email: 'anita.desai@company.com',
      phone: '9876543210',
      baseLocation: 'Mumbai',
      department: 'Engineering',
      designation: 'Senior Developer',
      skills: 'JavaScript,React,Node.js',
      availability: 'AVAILABLE',
      totalEventsParticipated: 12,
      avgFeedbackScore: 4.5,
    })
  }),

  // GET /api/volunteers/:employeeId/history - paginated participation history
  http.get('/api/volunteers/:employeeId/history', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '5')

    return HttpResponse.json({
      content: [
        {
          eventId: 'evt-001',
          eventName: 'Annual Volunteer Drive',
          eventCode: 'EVT-2024-001',
          eventDate: '2024-06-01',
          city: 'Mumbai',
          attendanceStatus: 'ATTENDED',
          registeredAt: '2024-05-01T09:00:00Z',
        },
        {
          eventId: 'evt-003',
          eventName: 'Beach Cleanup',
          eventCode: 'EVT-2024-003',
          eventDate: '2024-04-22',
          city: 'Chennai',
          attendanceStatus: 'ATTENDED',
          registeredAt: '2024-04-01T07:00:00Z',
        },
      ],
      totalElements: 2,
      totalPages: Math.ceil(2 / size),
      page,
      size,
    })
  }),

  // PUT /api/volunteers/:employeeId - update profile/availability
  http.put('/api/volunteers/:employeeId', async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json({
      id: 'c0000000-0000-0000-0000-000000000001',
      employeeId: params.employeeId,
      fullName: 'Anita Desai',
      email: 'anita.desai@company.com',
      phone: '9876543210',
      baseLocation: 'Mumbai',
      department: 'Engineering',
      designation: 'Senior Developer',
      skills: 'JavaScript,React,Node.js',
      availability: body.availability ?? 'AVAILABLE',
      totalEventsParticipated: 12,
      avgFeedbackScore: 4.5,
    })
  }),
]

export const volunteerErrorHandlers = {
  badRequest: http.put('/api/volunteers/:employeeId', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Invalid availability value',
        details: [{ field: 'availability', message: 'Must be AVAILABLE, BUSY, or ON_LEAVE' }],
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  unauthorized: http.get('/api/volunteers', () => {
    return HttpResponse.json(
      { error: 'Unauthorized', message: 'Token expired or invalid' },
      {
        status: 401,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  serverError: http.get('/api/volunteers', () => {
    return HttpResponse.json(
      { error: 'Internal Server Error', message: 'An unexpected error occurred' },
      {
        status: 500,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  notFound: http.get('/api/volunteers/:employeeId', () => {
    return HttpResponse.json(
      { error: 'Not Found', message: 'Volunteer not found' },
      {
        status: 404,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),
}
