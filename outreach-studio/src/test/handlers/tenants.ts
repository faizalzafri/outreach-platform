import { http, HttpResponse } from 'msw'

export const MOCK_TENANT = {
  id: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
  name: 'Acme Corporation',
  slug: 'acme-corp',
  status: 'ACTIVE',
  createdDate: '2025-01-15T10:00:00Z',
}

export const tenantHandlers = [
  // GET /api/tenants/current - current user's own tenant
  http.get('/api/tenants/current', () => {
    return HttpResponse.json(MOCK_TENANT)
  }),

  // GET /api/tenants - paginated, searchable list (Platform Admin only)
  http.get('/api/tenants', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '20')
    const search = url.searchParams.get('search')?.toLowerCase() ?? ''

    const allTenants = [
      MOCK_TENANT,
      {
        id: 'b2c3d4e5-f6a7-8901-bcde-f12345678901',
        name: 'Globex Industries',
        slug: 'globex',
        status: 'ACTIVE',
        createdDate: '2025-02-20T10:00:00Z',
      },
      {
        id: 'c3d4e5f6-a7b8-9012-cdef-123456789012',
        name: 'Initech Solutions',
        slug: 'initech',
        status: 'SUSPENDED',
        createdDate: '2025-03-10T10:00:00Z',
      },
    ]

    const filtered = search
      ? allTenants.filter((t) => t.name.toLowerCase().includes(search))
      : allTenants

    return HttpResponse.json({
      content: filtered,
      totalElements: filtered.length,
      totalPages: 1,
      page,
      size,
    })
  }),
]

export const tenantErrorHandlers = {
  currentNotFound: http.get('/api/tenants/current', () => {
    return HttpResponse.json(
      { error: 'TENANT_NOT_FOUND', message: 'Tenant not found' },
      { status: 404 },
    )
  }),
}
