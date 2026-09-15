import { http, HttpResponse } from 'msw'

export const ingestionHandlers = [
  // POST /api/ingestion/upload - file upload (returns 202)
  http.post('/api/ingestion/upload', () => {
    return HttpResponse.json(
      {
        jobId: 'job-001',
        status: 'ACCEPTED',
        fileName: 'volunteers-2024.xlsx',
        createdAt: new Date().toISOString(),
      },
      { status: 202 },
    )
  }),

  // GET /api/ingestion/jobs/:jobId - poll job status
  http.get('/api/ingestion/jobs/:jobId', ({ params }) => {
    return HttpResponse.json({
      id: params.jobId,
      status: 'COMPLETED',
      fileName: 'volunteers-2024.xlsx',
      progress: 100,
      totalRows: 250,
      processedRows: 250,
      errorCount: 3,
      createdAt: '2024-06-01T10:00:00Z',
      completedAt: '2024-06-01T10:02:30Z',
    })
  }),

  // GET /api/ingestion/jobs - list jobs
  http.get('/api/ingestion/jobs', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') ?? '0')
    const size = Number(url.searchParams.get('size') ?? '10')

    return HttpResponse.json({
      content: [
        {
          id: 'job-001',
          fileName: 'volunteers-2024.xlsx',
          status: 'COMPLETED',
          progress: 100,
          totalRows: 250,
          processedRows: 250,
          errorCount: 3,
          createdAt: '2024-06-01T10:00:00Z',
          completedAt: '2024-06-01T10:02:30Z',
        },
        {
          id: 'job-002',
          fileName: 'events-import.csv',
          status: 'RUNNING',
          progress: 65,
          totalRows: 100,
          processedRows: 65,
          errorCount: 0,
          createdAt: '2024-06-02T14:00:00Z',
          completedAt: null,
        },
      ],
      totalElements: 12,
      totalPages: Math.ceil(12 / size),
      page,
      size,
    })
  }),

  // GET /api/ingestion/jobs/:jobId/errors - job errors (bare array, matching the real backend)
  http.get('/api/ingestion/jobs/:jobId/errors', () => {
    return HttpResponse.json([
      {
        rowNumber: 15,
        columnName: 'email',
        errorMessage: 'Invalid email format',
        rejectedValue: 'invalid-email',
      },
      {
        rowNumber: 42,
        columnName: 'department',
        errorMessage: 'Department is required',
        rejectedValue: '',
      },
      {
        rowNumber: 108,
        columnName: 'employeeId',
        errorMessage: 'Duplicate employee ID',
        rejectedValue: 'EMP001',
      },
    ])
  }),
]

export const ingestionErrorHandlers = {
  badRequest: http.post('/api/ingestion/upload', () => {
    return HttpResponse.json(
      {
        error: 'Bad Request',
        message: 'Invalid file format. Supported: .xlsx, .xls, .csv',
      },
      {
        status: 400,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  unauthorized: http.post('/api/ingestion/upload', () => {
    return HttpResponse.json(
      { error: 'Unauthorized', message: 'Token expired or invalid' },
      {
        status: 401,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  serverError: http.get('/api/ingestion/jobs', () => {
    return HttpResponse.json(
      { error: 'Internal Server Error', message: 'An unexpected error occurred' },
      {
        status: 500,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),

  fileTooLarge: http.post('/api/ingestion/upload', () => {
    return HttpResponse.json(
      {
        error: 'Payload Too Large',
        message: 'File size exceeds the maximum allowed limit of 25MB',
      },
      {
        status: 413,
        headers: { 'X-Correlation-ID': 'test-correlation-id' },
      },
    )
  }),
}
