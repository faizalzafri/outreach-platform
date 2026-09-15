// AI Insights types — feature-toggled admin tooling backed by ai-service's async job API.

export type AiFeatureName = 'summarize' | 'anomalies' | 'query';

export interface AiStatus {
  provider: string;
  features: Record<AiFeatureName, boolean>;
  health: 'UP' | 'DEGRADED' | 'DISABLED';
}

export type AiJobStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface AiJob {
  jobId: string;
  status: AiJobStatus;
  result: string | null;
  createdAt: string;
}

export interface SubmitJobResponse {
  jobId: string;
  status: AiJobStatus;
  message: string;
}

export interface SummarizeRequest {
  context: string;
  maxLength?: number;
}

export interface AnomalyRequest {
  dataset: Array<Record<string, unknown>>;
  threshold?: number;
}

export interface QueryRequest {
  prompt: string;
  context?: string;
}
