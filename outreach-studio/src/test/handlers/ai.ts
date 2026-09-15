import { http, HttpResponse } from 'msw'

export const MOCK_AI_STATUS = {
  provider: 'openai',
  features: { summarize: true, anomalies: true, query: true },
  health: 'UP',
}

export const aiHandlers = [
  http.get('/api/ai/status', () => {
    return HttpResponse.json(MOCK_AI_STATUS)
  }),

  http.post('/api/ai/summarize', () => {
    return HttpResponse.json(
      { jobId: 'job-summarize-001', status: 'PENDING', message: 'Job submitted successfully' },
      { status: 202 },
    )
  }),

  http.post('/api/ai/anomalies', () => {
    return HttpResponse.json(
      { jobId: 'job-anomalies-001', status: 'PENDING', message: 'Job submitted successfully' },
      { status: 202 },
    )
  }),

  http.post('/api/ai/query', () => {
    return HttpResponse.json(
      { jobId: 'job-query-001', status: 'PENDING', message: 'Job submitted successfully' },
      { status: 202 },
    )
  }),

  http.get('/api/ai/jobs/:jobId', ({ params }) => {
    return HttpResponse.json({
      jobId: params.jobId,
      status: 'COMPLETED',
      result: 'Mock AI-generated result.',
      createdAt: '2025-01-15T10:00:00Z',
    })
  }),
]

export const aiErrorHandlers = {
  featureDisabled: (feature: string) =>
    http.post(`/api/ai/${feature}`, () => {
      return HttpResponse.json(
        {
          error: 'AI_FEATURE_DISABLED',
          message: `The AI feature '${feature}' is currently disabled`,
          feature,
        },
        { status: 501 },
      )
    }),

  jobFailed: http.get('/api/ai/jobs/:jobId', ({ params }) => {
    return HttpResponse.json({
      jobId: params.jobId,
      status: 'FAILED',
      result: 'The AI provider returned an error.',
      createdAt: '2025-01-15T10:00:00Z',
    })
  }),

  statusUnavailable: http.get('/api/ai/status', () => {
    return HttpResponse.json({ error: 'SERVER_ERROR', message: 'AI service unavailable' }, { status: 500 })
  }),
}
