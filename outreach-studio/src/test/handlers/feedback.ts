import { http, HttpResponse } from 'msw'

export const feedbackHandlers = [
  // POST /api/feedback - submit feedback
  http.post('/api/feedback', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json(
      {
        id: 'fb-001',
        ...body,
        submittedAt: new Date().toISOString(),
        status: 'SUBMITTED',
      },
      { status: 201 },
    )
  }),

  // GET /api/feedback - list feedback (paginated)
  http.get('/api/feedback', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '10')

    return HttpResponse.json({
      content: [
        {
          id: 'fb-001',
          eventId: 'evt-001',
          eventName: 'Annual Volunteer Drive',
          employeeId: 'EMP001',
          employeeName: 'Anita Desai',
          score: 5,
          textAnswer1: 'Great experience organizing the event.',
          textAnswer2: 'Loved the teamwork and coordination.',
          textAnswer3: 'Could improve the food arrangements.',
          category: 'EVENT_EXPERIENCE',
          anonymous: false,
          submittedAt: '2024-06-04T10:00:00Z',
        },
        {
          id: 'fb-002',
          eventId: 'evt-001',
          eventName: 'Annual Volunteer Drive',
          employeeId: 'EMP002',
          employeeName: 'Vikram Singh',
          score: 4,
          textAnswer1: 'Well-organized event with clear goals.',
          textAnswer2: 'The registration process was smooth.',
          textAnswer3: null,
          category: 'LOGISTICS',
          anonymous: true,
          submittedAt: '2024-06-04T11:30:00Z',
        },
      ],
      totalElements: 48,
      totalPages: Math.ceil(48 / size),
      page,
      size,
    })
  }),
]

export const feedbackErrorHandlers = {
  badRequest: http.post('/api/feedback', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Validation failed',
        details: [
          { field: 'score', message: 'Score must be between 1 and 5' },
          { field: 'textAnswer1', message: 'Text answer must be between 1 and 500 characters' },
        ],
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  unauthorized: http.get('/api/feedback', () => {
    return HttpResponse.json(
      { error: 'Unauthorized', message: 'Token expired or invalid' },
      {
        status: 401,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  serverError: http.post('/api/feedback', () => {
    return HttpResponse.json(
      { error: 'Internal Server Error', message: 'An unexpected error occurred' },
      {
        status: 500,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),
}
