import { useMutation, useQuery } from '@tanstack/react-query';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import type {
  AiStatus,
  AiJob,
  SubmitJobResponse,
  SummarizeRequest,
  AnomalyRequest,
  QueryRequest,
} from '@/types/ai';

const JOB_POLL_INTERVAL_MS = 1500;

/** Feature-toggle and provider health status, shown at the top of the AI Insights page. */
export function useAiStatus() {
  return useQuery<AiStatus>({
    queryKey: queryKeys.ai.status(),
    queryFn: async () => (await httpClient.get<AiStatus>('/ai/status')).data,
  });
}

/**
 * Polls a submitted job until it reaches a terminal state (COMPLETED/FAILED).
 * Pass `null` to disable — used before a job has been submitted yet.
 */
export function useAiJob(jobId: string | null) {
  return useQuery<AiJob>({
    queryKey: queryKeys.ai.job(jobId ?? 'none'),
    queryFn: async () => (await httpClient.get<AiJob>(`/ai/jobs/${jobId}`)).data,
    enabled: Boolean(jobId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'COMPLETED' || status === 'FAILED' ? false : JOB_POLL_INTERVAL_MS;
    },
  });
}

export function useSubmitSummarize() {
  return useMutation<SubmitJobResponse, unknown, SummarizeRequest>({
    mutationFn: async (payload) => (await httpClient.post<SubmitJobResponse>('/ai/summarize', payload)).data,
  });
}

export function useSubmitAnomalies() {
  return useMutation<SubmitJobResponse, unknown, AnomalyRequest>({
    mutationFn: async (payload) => (await httpClient.post<SubmitJobResponse>('/ai/anomalies', payload)).data,
  });
}

export function useSubmitQuery() {
  return useMutation<SubmitJobResponse, unknown, QueryRequest>({
    mutationFn: async (payload) => (await httpClient.post<SubmitJobResponse>('/ai/query', payload)).data,
  });
}
