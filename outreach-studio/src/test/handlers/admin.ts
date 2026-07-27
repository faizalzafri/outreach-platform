import { http, HttpResponse } from 'msw'

export const adminHandlers = [
  // GET /api/admin/users - list users
  http.get('/api/admin/users', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '10')

    return HttpResponse.json({
      content: [
        {
          id: 'user-001',
          username: 'priya_sharma',
          email: 'priya.sharma@company.com',
          role: 'ROLE_PMO',
          status: 'ACTIVE',
          lastLogin: '2024-06-01T09:30:00Z',
          createdAt: '2023-01-15T10:00:00Z',
          locked: false,
        },
        {
          id: 'user-002',
          username: 'rahul_verma',
          email: 'rahul.verma@company.com',
          role: 'ROLE_POC',
          status: 'ACTIVE',
          lastLogin: '2024-05-28T14:00:00Z',
          createdAt: '2023-03-20T08:00:00Z',
          locked: false,
        },
        {
          id: 'user-003',
          username: 'admin_user',
          email: 'admin@company.com',
          role: 'ROLE_ADMIN',
          status: 'ACTIVE',
          lastLogin: '2024-06-02T08:00:00Z',
          createdAt: '2022-06-01T00:00:00Z',
          locked: false,
        },
        {
          id: 'user-004',
          username: 'locked_user',
          email: 'locked@company.com',
          role: 'ROLE_POC',
          status: 'LOCKED',
          lastLogin: '2024-04-01T10:00:00Z',
          createdAt: '2023-06-01T00:00:00Z',
          locked: true,
        },
      ],
      totalElements: 35,
      totalPages: Math.ceil(35 / size),
      page,
      size,
    })
  }),

  // POST /api/admin/users - create user
  http.post('/api/admin/users', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json(
      {
        id: 'user-new-001',
        username: body.username,
        email: body.email,
        role: body.role,
        status: 'ACTIVE',
        lastLogin: null,
        createdAt: new Date().toISOString(),
        locked: false,
      },
      { status: 201 },
    )
  }),

  // PATCH /api/admin/users/:id/role - change role
  http.patch('/api/admin/users/:id/role', async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json({
      id: params.id,
      role: body.role,
      updatedAt: new Date().toISOString(),
    })
  }),

  // PATCH /api/admin/users/:id/status - change status
  http.patch('/api/admin/users/:id/status', async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json({
      id: params.id,
      status: body.status,
      updatedAt: new Date().toISOString(),
    })
  }),
]

export const adminErrorHandlers = {
  badRequest: http.post('/api/admin/users', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Validation failed',
        details: [
          { field: 'username', message: 'Username must be 3-50 alphanumeric characters or underscores' },
          { field: 'email', message: 'Invalid email format' },
        ],
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  unauthorized: http.get('/api/admin/users', () => {
    return HttpResponse.json(
      { error: 'Unauthorized', message: 'Token expired or invalid' },
      {
        status: 401,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  serverError: http.get('/api/admin/users', () => {
    return HttpResponse.json(
      { error: 'Internal Server Error', message: 'An unexpected error occurred' },
      {
        status: 500,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  duplicateUsername: http.post('/api/admin/users', () => {
    return HttpResponse.json(
      {
        error: 'Conflict',
        message: 'Username already exists',
        details: [{ field: 'username', message: 'A user with this username already exists' }],
      },
      {
        status: 409,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  duplicateEmail: http.post('/api/admin/users', () => {
    return HttpResponse.json(
      {
        error: 'Conflict',
        message: 'Email already exists',
        details: [{ field: 'email', message: 'A user with this email already exists' }],
      },
      {
        status: 409,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),
}
