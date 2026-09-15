/**
 * AI Insights Content (lazy-loaded)
 *
 * Three feature-toggled panels — Summarize Feedback, Detect Anomalies, Ask a
 * Question — each submitting a job to ai-service and polling it to
 * completion. A disabled feature (per /ai/status) blocks submission client
 * side; ai-service itself also rejects it with AI_FEATURE_DISABLED if
 * something slips through, so both layers agree.
 */

import { useState } from 'react';

import {
  useAiStatus,
  useAiJob,
  useSubmitSummarize,
  useSubmitAnomalies,
  useSubmitQuery,
} from '@/hooks/useAi';
import type { AiFeatureName, AiJob } from '@/types/ai';
import type { NormalizedError } from '@/types/api';

import styles from './AiInsightsContent.module.css';

// ---------------------------------------------------------------------------
// Shared job status/result display
// ---------------------------------------------------------------------------

function JobStatusPanel({ job, submitError }: { job: AiJob | undefined; submitError: string | null }) {
  if (submitError) {
    return (
      <div className={styles['resultBox']} role="alert">
        <p className={styles['resultError']}>{submitError}</p>
      </div>
    );
  }

  if (!job) return null;

  if (job.status === 'PENDING' || job.status === 'RUNNING') {
    return (
      <div className={styles['resultBox']} role="status">
        <span className={styles['spinner']} aria-hidden="true" />
        <span>{job.status === 'PENDING' ? 'Queued…' : 'Running…'}</span>
      </div>
    );
  }

  if (job.status === 'FAILED') {
    return (
      <div className={styles['resultBox']} role="alert">
        <p className={styles['resultError']}>{job.result ?? 'The job failed.'}</p>
      </div>
    );
  }

  return (
    <div className={styles['resultBox']}>
      <p className={styles['resultText']}>{job.result}</p>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Summarize Feedback panel
// ---------------------------------------------------------------------------

function SummarizePanel({ enabled }: { enabled: boolean }) {
  const [context, setContext] = useState('');
  const [jobId, setJobId] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const submitMutation = useSubmitSummarize();
  const jobQuery = useAiJob(jobId);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError(null);
    submitMutation.mutate(
      { context },
      {
        onSuccess: (response) => setJobId(response.jobId),
        onError: (error) => setSubmitError((error as NormalizedError).message || 'Failed to submit job'),
      },
    );
  }

  const isBusy = submitMutation.isPending || jobQuery.data?.status === 'PENDING' || jobQuery.data?.status === 'RUNNING';

  return (
    <section className={styles['panel']}>
      <h2 className={styles['panelTitle']}>Summarize Feedback</h2>
      <p className={styles['panelDescription']}>
        Paste raw feedback text (e.g. exported comments for an event) to generate a summary.
      </p>
      <form className={styles['form']} onSubmit={handleSubmit}>
        <textarea
          className={styles['textarea']}
          value={context}
          onChange={(e) => setContext(e.target.value)}
          placeholder="Paste feedback text to summarize…"
          maxLength={50000}
          rows={5}
          disabled={!enabled || isBusy}
          aria-label="Feedback text to summarize"
          required
        />
        <button
          type="submit"
          className={styles['submitBtn']}
          disabled={!enabled || isBusy || context.trim().length === 0}
        >
          {isBusy ? 'Summarizing…' : 'Summarize'}
        </button>
      </form>
      {!enabled && <p className={styles['disabledNotice']}>This feature is currently disabled.</p>}
      <JobStatusPanel job={jobQuery.data} submitError={submitError} />
    </section>
  );
}

// ---------------------------------------------------------------------------
// Detect Anomalies panel
// ---------------------------------------------------------------------------

function AnomaliesPanel({ enabled }: { enabled: boolean }) {
  const [datasetText, setDatasetText] = useState('');
  const [parseError, setParseError] = useState<string | null>(null);
  const [jobId, setJobId] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const submitMutation = useSubmitAnomalies();
  const jobQuery = useAiJob(jobId);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setParseError(null);
    setSubmitError(null);

    let dataset: Array<Record<string, unknown>>;
    try {
      const parsed: unknown = JSON.parse(datasetText);
      if (!Array.isArray(parsed)) {
        throw new Error('Dataset must be a JSON array of objects');
      }
      dataset = parsed as Array<Record<string, unknown>>;
    } catch {
      setParseError('Enter a valid JSON array of data points, e.g. [{"score": 5}, {"score": 1}]');
      return;
    }

    submitMutation.mutate(
      { dataset },
      {
        onSuccess: (response) => setJobId(response.jobId),
        onError: (error) => setSubmitError((error as NormalizedError).message || 'Failed to submit job'),
      },
    );
  }

  const isBusy = submitMutation.isPending || jobQuery.data?.status === 'PENDING' || jobQuery.data?.status === 'RUNNING';

  return (
    <section className={styles['panel']}>
      <h2 className={styles['panelTitle']}>Detect Anomalies</h2>
      <p className={styles['panelDescription']}>
        Paste a JSON array of data points (e.g. feedback scores) to flag statistical outliers.
      </p>
      <form className={styles['form']} onSubmit={handleSubmit}>
        <textarea
          className={styles['textarea']}
          value={datasetText}
          onChange={(e) => setDatasetText(e.target.value)}
          placeholder='[{"score": 5}, {"score": 1}, {"score": 4}]'
          rows={5}
          disabled={!enabled || isBusy}
          aria-label="Dataset (JSON array)"
          aria-describedby={parseError ? 'anomaly-parse-error' : undefined}
          required
        />
        {parseError && (
          <p id="anomaly-parse-error" className={styles['fieldError']} role="alert">
            {parseError}
          </p>
        )}
        <button
          type="submit"
          className={styles['submitBtn']}
          disabled={!enabled || isBusy || datasetText.trim().length === 0}
        >
          {isBusy ? 'Analyzing…' : 'Detect Anomalies'}
        </button>
      </form>
      {!enabled && <p className={styles['disabledNotice']}>This feature is currently disabled.</p>}
      <JobStatusPanel job={jobQuery.data} submitError={submitError} />
    </section>
  );
}

// ---------------------------------------------------------------------------
// Ask a Question panel
// ---------------------------------------------------------------------------

function QueryPanel({ enabled }: { enabled: boolean }) {
  const [prompt, setPrompt] = useState('');
  const [context, setContext] = useState('');
  const [jobId, setJobId] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const submitMutation = useSubmitQuery();
  const jobQuery = useAiJob(jobId);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError(null);
    submitMutation.mutate(
      { prompt, context: context.trim() || undefined },
      {
        onSuccess: (response) => setJobId(response.jobId),
        onError: (error) => setSubmitError((error as NormalizedError).message || 'Failed to submit job'),
      },
    );
  }

  const isBusy = submitMutation.isPending || jobQuery.data?.status === 'PENDING' || jobQuery.data?.status === 'RUNNING';

  return (
    <section className={styles['panel']}>
      <h2 className={styles['panelTitle']}>Ask a Question</h2>
      <p className={styles['panelDescription']}>
        Ask a natural-language question about platform data (e.g. feedback trends).
      </p>
      <form className={styles['form']} onSubmit={handleSubmit}>
        <textarea
          className={styles['textarea']}
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          placeholder="What is the average feedback score for events in Bangalore?"
          maxLength={2000}
          rows={2}
          disabled={!enabled || isBusy}
          aria-label="Question"
          required
        />
        <textarea
          className={styles['textarea']}
          value={context}
          onChange={(e) => setContext(e.target.value)}
          placeholder="Optional context to scope the question…"
          rows={2}
          disabled={!enabled || isBusy}
          aria-label="Optional context"
        />
        <button
          type="submit"
          className={styles['submitBtn']}
          disabled={!enabled || isBusy || prompt.trim().length === 0}
        >
          {isBusy ? 'Asking…' : 'Ask'}
        </button>
      </form>
      {!enabled && <p className={styles['disabledNotice']}>This feature is currently disabled.</p>}
      <JobStatusPanel job={jobQuery.data} submitError={submitError} />
    </section>
  );
}

// ---------------------------------------------------------------------------
// Status banner
// ---------------------------------------------------------------------------

/**
 * Whether a panel should allow submission. Defaults to true while status is still
 * loading/unknown (avoids briefly flashing "disabled" on every page load), and only
 * blocks once the server has explicitly reported the feature as off.
 */
function featureEnabled(features: Partial<Record<AiFeatureName, boolean>> | undefined, name: AiFeatureName): boolean {
  return features?.[name] ?? true;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function AiInsightsContent() {
  const statusQuery = useAiStatus();

  return (
    <div className={styles['container']}>
      <h1 className={styles['pageTitle']}>AI Insights</h1>

      {statusQuery.isLoading && <p className={styles['statusLine']}>Checking AI service status…</p>}

      {statusQuery.isError && (
        <div className={styles['statusBanner']} role="alert">
          Could not reach the AI service. Panels below may not work until it's back.
        </div>
      )}

      {statusQuery.data && (
        <div
          className={`${styles['statusBanner']} ${styles[`statusBanner--${statusQuery.data.health.toLowerCase()}`]}`}
        >
          Provider: <strong>{statusQuery.data.provider}</strong> · Status:{' '}
          <strong>{statusQuery.data.health}</strong>
        </div>
      )}

      <div className={styles['panels']}>
        <SummarizePanel enabled={featureEnabled(statusQuery.data?.features, 'summarize')} />
        <AnomaliesPanel enabled={featureEnabled(statusQuery.data?.features, 'anomalies')} />
        <QueryPanel enabled={featureEnabled(statusQuery.data?.features, 'query')} />
      </div>
    </div>
  );
}
