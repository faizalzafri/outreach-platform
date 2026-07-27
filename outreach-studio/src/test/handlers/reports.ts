import { http, HttpResponse } from 'msw'

export const reportHandlers = [
  // GET /api/reports/dashboard/kpis - dashboard KPIs
  http.get('/api/reports/dashboard/kpis', () => {
    return HttpResponse.json({
      totalEvents: 42,
      activeEvents: 8,
      totalVolunteers: 356,
      avgFeedbackScore: 4.3,
      pendingFeedback: 12,
      notificationDeliveryRate: 97.5,
    })
  }),

  // GET /api/reports/dashboard/trends - trend data
  http.get('/api/reports/dashboard/trends', ({ request }) => {
    const url = new URL(request.url)
    const granularity = url.searchParams.get('granularity') ?? 'day'
    // Use granularity to shape response (simplified for mock)
    void granularity

    return HttpResponse.json({
      feedbackTrends: [
        { date: '2024-05-01', count: 12, avgScore: 4.1 },
        { date: '2024-05-02', count: 8, avgScore: 4.5 },
        { date: '2024-05-03', count: 15, avgScore: 3.9 },
        { date: '2024-05-04', count: 10, avgScore: 4.2 },
        { date: '2024-05-05', count: 6, avgScore: 4.7 },
      ],
      eventStatusDistribution: [
        { status: 'DRAFT', count: 5 },
        { status: 'PUBLISHED', count: 8 },
        { status: 'ACTIVE', count: 12 },
        { status: 'COMPLETED', count: 15 },
        { status: 'ARCHIVED', count: 2 },
      ],
      feedbackScoreDistribution: [
        { score: 1, count: 3 },
        { score: 2, count: 8 },
        { score: 3, count: 22 },
        { score: 4, count: 45 },
        { score: 5, count: 32 },
      ],
    })
  }),

  // POST /api/reports/export - start export job
  http.post('/api/reports/export', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json(
      {
        jobId: 'export-001',
        format: body.format,
        status: 'PROCESSING',
        createdAt: new Date().toISOString(),
      },
      { status: 202 },
    )
  }),

  // GET /api/reports/export/:jobId - poll export status
  http.get('/api/reports/export/:jobId', ({ params }) => {
    return HttpResponse.json({
      jobId: params.jobId,
      status: 'COMPLETED',
      format: 'PDF',
      downloadUrl: `/api/reports/export/${String(params.jobId)}/download`,
      fileSize: 245_760,
      createdAt: '2024-06-01T10:00:00Z',
      completedAt: '2024-06-01T10:00:15Z',
    })
  }),
]

export const reportErrorHandlers = {
  badRequest: http.post('/api/reports/export', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Invalid export parameters',
        details: [{ field: 'format', message: 'Format must be one of: PDF, CSV, EXCEL' }],
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  unauthorized: http.get('/api/reports/dashboard/kpis', () => {
    return HttpResponse.json(
      { error: 'Unauthorized', message: 'Token expired or invalid' },
      {
        status: 401,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  serverError: http.get('/api/reports/dashboard/trends', () => {
    return HttpResponse.json(
      { error: 'Internal Server Error', message: 'An unexpected error occurred' },
      {
        status: 500,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  exportFailed: http.get('/api/reports/export/:jobId', ({ params }) => {
    return HttpResponse.json({
      jobId: params.jobId,
      status: 'FAILED',
      error: 'Report generation timed out',
      createdAt: '2024-06-01T10:00:00Z',
      failedAt: '2024-06-01T10:02:00Z',
    })
  }),
}
