import { http, HttpResponse } from 'msw'

export const MOCK_TEAMS = [
  {
    id: 'team-001',
    name: 'Community Outreach',
    description: 'Coordinates volunteer events across the region',
    memberCount: 3,
    createdDate: '2025-01-10T10:00:00Z',
  },
  {
    id: 'team-002',
    name: 'Corporate Partnerships',
    description: 'Manages relationships with corporate sponsors',
    memberCount: 2,
    createdDate: '2025-02-05T10:00:00Z',
  },
]

export const MOCK_TEAM_MEMBERS = [
  {
    userId: 'user-101',
    username: 'priya_sharma',
    email: 'priya.sharma@company.com',
    joinedAt: '2025-01-11T09:00:00Z',
  },
  {
    userId: 'user-102',
    username: 'rahul_verma',
    email: 'rahul.verma@company.com',
    joinedAt: '2025-01-12T09:00:00Z',
  },
]

export const MOCK_AVAILABLE_USERS = [
  { id: 'user-201', username: 'anita_desai', email: 'anita.desai@company.com' },
  { id: 'user-202', username: 'anand_kumar', email: 'anand.kumar@company.com' },
]

export const teamHandlers = [
  // GET /api/teams - paginated list
  http.get('/api/teams', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '20')

    return HttpResponse.json({
      content: MOCK_TEAMS,
      totalElements: MOCK_TEAMS.length,
      totalPages: 1,
      page,
      size,
    })
  }),

  // POST /api/teams - create team
  http.post('/api/teams', async ({ request }) => {
    const body = (await request.json()) as { name: string; description?: string }
    return HttpResponse.json(
      {
        id: 'team-new-001',
        name: body.name,
        description: body.description ?? '',
        memberCount: 0,
        createdDate: new Date().toISOString(),
      },
      { status: 201 },
    )
  }),

  // GET /api/teams/:id - team detail
  http.get('/api/teams/:id', ({ params }) => {
    const team = MOCK_TEAMS.find((t) => t.id === params.id) ?? MOCK_TEAMS[0]!
    return HttpResponse.json(team)
  }),

  // PUT /api/teams/:id - update team
  http.put('/api/teams/:id', async ({ params, request }) => {
    const body = (await request.json()) as { name: string; description?: string }
    const existing = MOCK_TEAMS.find((t) => t.id === params.id) ?? MOCK_TEAMS[0]!
    return HttpResponse.json({
      ...existing,
      name: body.name,
      description: body.description ?? '',
    })
  }),

  // DELETE /api/teams/:id - delete team
  http.delete('/api/teams/:id', () => {
    return new HttpResponse(null, { status: 204 })
  }),

  // GET /api/teams/:id/members - paginated member list
  http.get('/api/teams/:id/members', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '20')

    return HttpResponse.json({
      content: MOCK_TEAM_MEMBERS,
      totalElements: MOCK_TEAM_MEMBERS.length,
      totalPages: 1,
      page,
      size,
    })
  }),

  // POST /api/teams/:id/members - add member
  http.post('/api/teams/:id/members', async ({ request }) => {
    const body = (await request.json()) as { userId: string }
    const user = MOCK_AVAILABLE_USERS.find((u) => u.id === body.userId)
    return HttpResponse.json(
      {
        userId: body.userId,
        username: user?.username ?? 'new_member',
        email: user?.email ?? 'new_member@example.com',
        joinedAt: new Date().toISOString(),
      },
      { status: 201 },
    )
  }),

  // DELETE /api/teams/:id/members/:userId - remove member
  http.delete('/api/teams/:id/members/:userId', () => {
    return new HttpResponse(null, { status: 204 })
  }),

  // GET /api/teams/:id/available-users - searchable user selector
  http.get('/api/teams/:id/available-users', ({ request }) => {
    const url = new URL(request.url)
    const search = url.searchParams.get('search')?.toLowerCase() ?? ''
    const filtered = search
      ? MOCK_AVAILABLE_USERS.filter((u) => u.username.toLowerCase().includes(search))
      : MOCK_AVAILABLE_USERS
    return HttpResponse.json(filtered)
  }),
]

export const teamErrorHandlers = {
  duplicateName: http.post('/api/teams', () => {
    return HttpResponse.json(
      {
        error: 'Conflict',
        message: 'A team with this name already exists',
        fieldErrors: [{ field: 'name', message: 'Team name already in use' }],
      },
      { status: 409 },
    )
  }),

  listServerError: http.get('/api/teams', () => {
    return HttpResponse.json(
      { error: 'SERVER_ERROR', message: 'Failed to load teams' },
      { status: 500 },
    )
  }),

  detailNotFound: http.get('/api/teams/:id', () => {
    return HttpResponse.json(
      { error: 'NOT_FOUND', message: 'Team not found' },
      { status: 404 },
    )
  }),

  deleteFailed: http.delete('/api/teams/:id', () => {
    return HttpResponse.json(
      { error: 'CONFLICT', message: 'Team could not be deleted' },
      { status: 409 },
    )
  }),

  addMemberFailed: http.post('/api/teams/:id/members', () => {
    return HttpResponse.json(
      { error: 'CONFLICT', message: 'User is already a member of this team' },
      { status: 409 },
    )
  }),
}
