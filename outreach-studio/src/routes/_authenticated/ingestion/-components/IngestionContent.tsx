/**
 * Ingestion Content (lazy-loaded)
 *
 * Provides file upload with drag-and-drop, upload progress tracking,
 * job status polling, and a recent jobs list.
 */

import { useState, useCallback, useRef, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { type ColumnDef } from '@tanstack/react-table';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useExponentialPolling } from '@/hooks/useExponentialPolling';
import { DataTable } from '@/components/data-table/DataTable';
import type { ImportJob } from '@/types/domain';

import styles from './IngestionContent.module.css';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface JobError {
  rowNumber: number;
  fieldName: string;
  message: string;
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const ACCEPTED_EXTENSIONS = ['.xlsx', '.xls', '.csv'];
const ACCEPTED_MIME_TYPES = [
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.ms-excel',
  'text/csv',
];
const MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB
const BASE_POLL_INTERVAL = 3000; // 3 seconds initial
const MAX_POLL_INTERVAL = 30000; // 30 seconds cap
const MAX_POLL_ATTEMPTS = 60;

const TERMINAL_STATUSES = new Set(['COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED']);

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function isValidFile(file: File): { valid: boolean; error?: string } {
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
  if (!ACCEPTED_EXTENSIONS.includes(ext)) {
    return { valid: false, error: `Unsupported file type "${ext}". Accepted: .xlsx, .xls, .csv` };
  }
  if (file.size > MAX_FILE_SIZE) {
    return { valid: false, error: `File exceeds maximum size of 25MB (${(file.size / (1024 * 1024)).toFixed(1)}MB)` };
  }
  return { valid: true };
}

function getStatusClass(status: string): string {
  switch (status) {
    case 'PENDING': return styles['statusPending']!;
    case 'IN_PROGRESS': return styles['statusInProgress']!;
    case 'COMPLETED': return styles['statusCompleted']!;
    case 'COMPLETED_WITH_ERRORS': return styles['statusCompletedWithErrors']!;
    case 'FAILED': return styles['statusFailed']!;
    default: return '';
  }
}

// ---------------------------------------------------------------------------
// Upload Hook
// ---------------------------------------------------------------------------

interface UploadState {
  uploading: boolean;
  uploadProgress: number;
  polling: boolean;
  pollCount: number;
  jobId: string | null;
  job: ImportJob | null;
  error: string | null;
  timeout: boolean;
}

function useFileUpload() {
  const [state, setState] = useState<UploadState>({
    uploading: false,
    uploadProgress: 0,
    polling: false,
    pollCount: 0,
    jobId: null,
    job: null,
    error: null,
    timeout: false,
  });

  const queryClient = useQueryClient();
  const jobIdRef = useRef<string | null>(null);

  // Exponential backoff polling: 3s → 6s → 12s → 24s → 30s (capped)
  const polling = useExponentialPolling<ImportJob>({
    queryFn: async () => {
      const response = await httpClient.get<ImportJob>(`/ingestion/jobs/${jobIdRef.current}`);
      return response.data;
    },
    isTerminal: (job) => TERMINAL_STATUSES.has(job.status),
    baseInterval: BASE_POLL_INTERVAL,
    maxInterval: MAX_POLL_INTERVAL,
    maxAttempts: MAX_POLL_ATTEMPTS,
    enabled: state.polling && !!state.jobId,
    onData: (job) => {
      setState((prev) => ({
        ...prev,
        job,
        pollCount: polling.attemptCount,
      }));
    },
    onComplete: () => {
      setState((prev) => ({ ...prev, polling: false }));
      void queryClient.invalidateQueries({ queryKey: queryKeys.ingestion.jobs() });
    },
    onError: () => {
      // Transient errors are retried by the hook with exponential backoff.
      // Only display a soft error; polling continues automatically.
      setState((prev) => ({
        ...prev,
        error: 'Temporary issue checking job status. Retrying...',
      }));
    },
    onTimeout: () => {
      setState((prev) => ({ ...prev, polling: false, timeout: true }));
    },
  });

  // Upload mutation
  const uploadMutation = useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append('file', file);

      const response = await httpClient.post<{ jobId: string }>('/ingestion/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          const percent = progressEvent.total
            ? Math.round((progressEvent.loaded / progressEvent.total) * 100)
            : 0;
          setState((prev) => ({ ...prev, uploadProgress: percent }));
        },
      });
      return response.data;
    },
    onMutate: () => {
      setState({
        uploading: true,
        uploadProgress: 0,
        polling: false,
        pollCount: 0,
        jobId: null,
        job: null,
        error: null,
        timeout: false,
      });
    },
    onSuccess: (data) => {
      jobIdRef.current = data.jobId;
      setState((prev) => ({
        ...prev,
        uploading: false,
        uploadProgress: 100,
        jobId: data.jobId,
        polling: true,
      }));
    },
    onError: (err) => {
      setState((prev) => ({
        ...prev,
        uploading: false,
        error: (err as { message?: string })?.message ?? 'Upload failed. Please try again.',
      }));
    },
  });

  const upload = useCallback((file: File) => {
    uploadMutation.mutate(file);
  }, [uploadMutation]);

  const reset = useCallback(() => {
    polling.stop();
    jobIdRef.current = null;
    setState({
      uploading: false,
      uploadProgress: 0,
      polling: false,
      pollCount: 0,
      jobId: null,
      job: null,
      error: null,
      timeout: false,
    });
  }, [polling]);

  return { state, upload, reset };
}

// ---------------------------------------------------------------------------
// Job List Columns
// ---------------------------------------------------------------------------

const jobColumns: ColumnDef<ImportJob, unknown>[] = [
  {
    accessorKey: 'id',
    header: 'Job ID',
    cell: ({ getValue }) => {
      const id = getValue() as string;
      return id.substring(0, 8) + '…';
    },
  },
  {
    accessorKey: 'filename',
    header: 'Filename',
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ getValue }) => {
      const status = getValue() as string;
      return (
        <span className={`${styles['statusBadge']} ${getStatusClass(status)}`}>
          {status.replace(/_/g, ' ')}
        </span>
      );
    },
    meta: {
      filterType: 'select' as const,
      filterOptions: [
        { label: 'Pending', value: 'PENDING' },
        { label: 'In Progress', value: 'IN_PROGRESS' },
        { label: 'Completed', value: 'COMPLETED' },
        { label: 'With Errors', value: 'COMPLETED_WITH_ERRORS' },
        { label: 'Failed', value: 'FAILED' },
      ],
    },
  },
  {
    accessorKey: 'progress',
    header: 'Progress',
    cell: ({ getValue }) => `${getValue() as number}%`,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'totalRows',
    header: 'Total Rows',
    enableColumnFilter: false,
  },
  {
    accessorKey: 'errorCount',
    header: 'Errors',
    enableColumnFilter: false,
  },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ getValue }) => {
      const date = new Date(getValue() as string);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    },
    enableColumnFilter: false,
  },
];

// ---------------------------------------------------------------------------
// Job Error Details — expandable section
// ---------------------------------------------------------------------------

function JobErrorDetails({ jobId }: { jobId: string }) {
  const [expanded, setExpanded] = useState(false);

  const { data: errors, isLoading, isError } = useQuery<JobError[]>({
    queryKey: queryKeys.ingestion.jobErrors(jobId),
    queryFn: async () => {
      const response = await httpClient.get<JobError[]>(`/ingestion/jobs/${jobId}/errors`);
      return response.data;
    },
    enabled: expanded,
  });

  return (
    <div style={{ marginTop: '0.75rem' }}>
      <button
        type="button"
        onClick={() => setExpanded((prev) => !prev)}
        style={{
          padding: '0.375rem 0.75rem',
          fontSize: '0.8125rem',
          cursor: 'pointer',
          border: '1px solid var(--border-default)',
          borderRadius: 'var(--radius-md)',
          background: 'var(--bg-surface)',
          color: 'var(--text-danger, #dc2626)',
        }}
      >
        {expanded ? 'Hide Errors' : 'View Errors'}
      </button>

      {expanded && (
        <div style={{ marginTop: '0.5rem' }}>
          {isLoading && <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Loading errors...</p>}
          {isError && <p style={{ fontSize: '0.8125rem', color: 'var(--text-danger, #dc2626)' }}>Failed to load errors.</p>}
          {errors && errors.length > 0 && (
            <table className={styles['errorTable'] ?? ''} style={{ width: '100%', fontSize: '0.8125rem', borderCollapse: 'collapse', marginTop: '0.25rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-default)' }}>
                  <th style={{ textAlign: 'left', padding: '0.375rem 0.5rem' }}>Row</th>
                  <th style={{ textAlign: 'left', padding: '0.375rem 0.5rem' }}>Field</th>
                  <th style={{ textAlign: 'left', padding: '0.375rem 0.5rem' }}>Message</th>
                </tr>
              </thead>
              <tbody>
                {errors.map((err, idx) => (
                  <tr key={idx} style={{ borderBottom: '1px solid var(--border-subtle, #e5e7eb)' }}>
                    <td style={{ padding: '0.375rem 0.5rem' }}>{err.rowNumber}</td>
                    <td style={{ padding: '0.375rem 0.5rem' }}>{err.fieldName}</td>
                    <td style={{ padding: '0.375rem 0.5rem' }}>{err.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {errors && errors.length === 0 && (
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>No error details available.</p>
          )}
        </div>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Main Component
// ---------------------------------------------------------------------------

export function IngestionContent() {
  const { state, upload, reset } = useFileUpload();
  const [dragActive, setDragActive] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [lastFile, setLastFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isDisabled = state.uploading || state.polling;

  const handleFile = useCallback((file: File) => {
    setValidationError(null);
    const result = isValidFile(file);
    if (!result.valid) {
      setValidationError(result.error ?? 'Invalid file');
      return;
    }
    setLastFile(file);
    upload(file);
  }, [upload]);

  const handleRetry = useCallback(() => {
    if (lastFile) {
      upload(lastFile);
    }
  }, [lastFile, upload]);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
    if (isDisabled) return;
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }, [handleFile, isDisabled]);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    if (!isDisabled) setDragActive(true);
  }, [isDisabled]);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
  }, []);

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
    // Reset the input so the same file can be re-selected
    e.target.value = '';
  }, [handleFile]);

  const handleZoneClick = useCallback(() => {
    if (!isDisabled) {
      fileInputRef.current?.click();
    }
  }, [isDisabled]);

  const jobListQueryKey = useMemo(() => queryKeys.ingestion.jobs(), []);

  return (
    <div className={styles['container']}>
      <h1 className={styles['pageTitle']}>Data Ingestion</h1>

      {/* Upload Section */}
      <section className={styles['uploadSection']}>
        <div
          className={`${styles['dropzone']} ${dragActive ? styles['dropzoneActive'] : ''} ${isDisabled ? styles['dropzoneDisabled'] : ''}`}
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onClick={handleZoneClick}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleZoneClick(); }}
          aria-label="Upload file drop zone"
        >
          <svg
            className={styles['dropzoneIcon']}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
            />
          </svg>
          <p className={styles['dropzoneText']}>
            Drag and drop your file here, or click to browse
          </p>
          <p className={styles['dropzoneHint']}>
            Accepted: .xlsx, .xls, .csv (max 25MB)
          </p>
          <input
            ref={fileInputRef}
            type="file"
            className={styles['fileInput']}
            accept={ACCEPTED_MIME_TYPES.join(',')}
            onChange={handleFileSelect}
            aria-hidden="true"
            tabIndex={-1}
          />
        </div>

        {/* Validation error */}
        {validationError && (
          <div className={styles['errorMessage']} role="alert">
            {validationError}
          </div>
        )}

        {/* Upload/Polling progress */}
        {(state.uploading || state.job || state.polling) && (
          <div className={styles['progressSection']}>
            <div className={styles['progressHeader']}>
              <p className={styles['progressLabel']}>
                {state.uploading
                  ? 'Uploading...'
                  : state.polling
                    ? 'Processing...'
                    : state.job
                      ? `Job ${state.job.status.replace(/_/g, ' ').toLowerCase()}`
                      : ''}
              </p>
              <span className={styles['progressPercent']}>
                {state.uploading
                  ? `${state.uploadProgress}%`
                  : state.job
                    ? `${state.job.progress}%`
                    : ''}
              </span>
            </div>

            <div className={styles['progressBarContainer']}>
              <div
                className={styles['progressBar']}
                style={{
                  width: `${state.uploading ? state.uploadProgress : (state.job?.progress ?? 0)}%`,
                }}
                role="progressbar"
                aria-valuenow={state.uploading ? state.uploadProgress : (state.job?.progress ?? 0)}
                aria-valuemin={0}
                aria-valuemax={100}
              />
            </div>

            {state.job && (
              <div className={styles['progressInfo']}>
                <span className={styles['progressInfoItem']}>
                  Status: <span className={styles['progressInfoValue']}>{state.job.status.replace(/_/g, ' ')}</span>
                </span>
                <span className={styles['progressInfoItem']}>
                  Rows: <span className={styles['progressInfoValue']}>{state.job.totalRows}</span>
                </span>
                {state.job.errorCount > 0 && (
                  <span className={styles['progressInfoItem']}>
                    Errors: <span className={styles['progressInfoValue']}>{state.job.errorCount}</span>
                  </span>
                )}
              </div>
            )}

            {!state.uploading && !state.polling && state.job && (
              <button
                type="button"
                onClick={reset}
                style={{ marginTop: '0.75rem', padding: '0.375rem 0.75rem', fontSize: '0.8125rem', cursor: 'pointer', border: '1px solid var(--border-default)', borderRadius: 'var(--radius-md)', background: 'var(--bg-surface)' }}
              >
                Upload another file
              </button>
            )}

            {/* View Errors button + expandable error details */}
            {!state.uploading && !state.polling && state.job && TERMINAL_STATUSES.has(state.job.status) && state.job.errorCount > 0 && (
              <JobErrorDetails jobId={state.job.id} />
            )}
          </div>
        )}

        {/* Upload error with retry */}
        {state.error && (
          <div className={styles['errorMessage']} role="alert">
            <span>{state.error}</span>
            {lastFile && (
              <button
                type="button"
                className={styles['retryButton']}
                onClick={handleRetry}
              >
                Retry Upload
              </button>
            )}
          </div>
        )}

        {/* Timeout message */}
        {state.timeout && (
          <div className={styles['timeoutMessage']} role="alert">
            Processing is taking longer than expected. The job may still complete — check the job list below for updates.
          </div>
        )}
      </section>

      {/* Job List Section */}
      <section className={styles['jobListSection']}>
        <h2 className={styles['sectionTitle']}>Recent Import Jobs</h2>
        <DataTable<ImportJob>
          columns={jobColumns}
          queryKey={jobListQueryKey}
          endpoint="/ingestion/jobs"
          defaultPageSize={10}
          emptyMessage="No import jobs yet. Upload a file to get started."
        />
      </section>
    </div>
  );
}
