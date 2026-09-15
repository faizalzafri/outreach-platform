import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { IngestionContent } from '../IngestionContent';

// Mock useAuth to provide authenticated user context
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: { sub: '1', name: 'Admin', email: 'admin@test.com', roles: ['ROLE_ADMIN'] },
    isAuthenticated: true,
    isLoading: false,
    login: vi.fn(),
    logout: vi.fn(),
  }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// Mock DataTable to avoid complex rendering of the job list
vi.mock('@/components/data-table/DataTable', () => ({
  DataTable: ({ emptyMessage }: { emptyMessage?: string }) => (
    <div data-testid="data-table">{emptyMessage ?? 'DataTable'}</div>
  ),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

function renderIngestion() {
  return render(<IngestionContent />, { wrapper: createWrapper() });
}

function createFile(name: string, sizeBytes: number, type = 'text/csv') {
  const content = new Uint8Array(sizeBytes);
  return new File([content], name, { type });
}

describe('IngestionContent', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('File validation', () => {
    it('rejects files with unsupported extensions', async () => {
      renderIngestion();

      const file = createFile('document.pdf', 1024, 'application/pdf');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      expect(screen.getByRole('alert')).toHaveTextContent(
        'Unsupported file type ".pdf". Accepted: .xlsx, .xls, .csv'
      );
    });

    it('rejects files exceeding 25MB size limit', async () => {
      renderIngestion();

      const oversizedFile = createFile('big-file.xlsx', 26 * 1024 * 1024,
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [oversizedFile] },
        });
      });

      expect(screen.getByRole('alert')).toHaveTextContent(/exceeds maximum size of 25MB/i);
    });

    it('accepts valid .csv files within size limit', async () => {
      renderIngestion();

      const file = createFile('volunteers.csv', 1024, 'text/csv');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      // No validation error should appear
      expect(screen.queryByText(/unsupported file type/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/exceeds maximum size/i)).not.toBeInTheDocument();
    });

    it('accepts valid .xlsx files', async () => {
      renderIngestion();

      const file = createFile('data.xlsx', 5000,
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      expect(screen.queryByText(/unsupported file type/i)).not.toBeInTheDocument();
    });

    it('accepts valid .xls files', async () => {
      renderIngestion();

      const file = createFile('legacy.xls', 2048, 'application/vnd.ms-excel');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      expect(screen.queryByText(/unsupported file type/i)).not.toBeInTheDocument();
    });
  });

  describe('Upload progress display', () => {
    it('shows uploading state with progress bar after valid file drop', async () => {
      renderIngestion();

      const file = createFile('test.csv', 1024, 'text/csv');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      // Should show uploading state or transition to polling
      await waitFor(() => {
        const progressBar = screen.queryByRole('progressbar');
        const uploadingText = screen.queryByText(/uploading|processing/i);
        expect(progressBar || uploadingText).toBeTruthy();
      });
    });

    it('displays progress bar with correct aria attributes', async () => {
      renderIngestion();

      const file = createFile('test.csv', 1024, 'text/csv');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      await waitFor(() => {
        const progressBar = screen.getByRole('progressbar');
        expect(progressBar).toHaveAttribute('aria-valuemin', '0');
        expect(progressBar).toHaveAttribute('aria-valuemax', '100');
      });
    });

    it('shows error message with retry button when upload fails', async () => {
      server.use(
        http.post('/api/ingestion/upload', () => {
          return HttpResponse.json(
            { error: 'Server Error', message: 'Upload failed' },
            { status: 500 }
          );
        })
      );

      renderIngestion();

      const file = createFile('test.csv', 1024, 'text/csv');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });

      // Should show retry button
      expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    });
  });

  describe('Polling lifecycle', () => {
    it('starts polling after successful upload and shows completion', async () => {
      // First upload returns 202 with jobId, then polling returns COMPLETED
      let _pollCount = 0;
      server.use(
        http.post('/api/ingestion/upload', () => {
          return HttpResponse.json(
            { jobId: 'test-job-123' },
            { status: 202 }
          );
        }),
        http.get('/api/ingestion/jobs/:jobId', () => {
          _pollCount++;
          return HttpResponse.json({
            id: 'test-job-123',
            status: 'COMPLETED',
            fileName: 'test.csv',
            progress: 100,
            totalRows: 50,
            errorCount: 0,
            createdAt: '2024-01-01T00:00:00Z',
          });
        })
      );

      renderIngestion();

      const file = createFile('test.csv', 1024, 'text/csv');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      // Advance timer to trigger polling
      await act(async () => {
        vi.advanceTimersByTime(4000);
      });

      await waitFor(() => {
        // Job should show COMPLETED status in the progress info section
        expect(screen.getByText('COMPLETED')).toBeInTheDocument();
      });
    });

    it('shows in-progress job status during polling', async () => {
      server.use(
        http.post('/api/ingestion/upload', () => {
          return HttpResponse.json(
            { jobId: 'test-job-456' },
            { status: 202 }
          );
        }),
        http.get('/api/ingestion/jobs/:jobId', () => {
          return HttpResponse.json({
            id: 'test-job-456',
            status: 'RUNNING',
            fileName: 'data.xlsx',
            progress: 45,
            totalRows: 100,
            errorCount: 0,
            createdAt: '2024-01-01T00:00:00Z',
          });
        })
      );

      renderIngestion();

      const file = createFile('data.xlsx', 1024,
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      // Advance timer to trigger first poll
      await act(async () => {
        vi.advanceTimersByTime(4000);
      });

      await waitFor(() => {
        expect(screen.getByText(/processing/i)).toBeInTheDocument();
        expect(screen.getByText('45%')).toBeInTheDocument();
      });
    });

    it('shows timeout message when max poll attempts exhausted', async () => {
      server.use(
        http.post('/api/ingestion/upload', () => {
          return HttpResponse.json(
            { jobId: 'test-job-timeout' },
            { status: 202 }
          );
        }),
        http.get('/api/ingestion/jobs/:jobId', () => {
          return HttpResponse.json({
            id: 'test-job-timeout',
            status: 'RUNNING',
            fileName: 'big-file.csv',
            progress: 20,
            totalRows: 10000,
            errorCount: 0,
            createdAt: '2024-01-01T00:00:00Z',
          });
        })
      );

      renderIngestion();

      const file = createFile('big-file.csv', 1024, 'text/csv');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      // Advance enough time for max attempts (60 attempts with exponential backoff)
      // The cap is 30s, so after the first few doublings, every poll is at 30s.
      // Total time needed ~ 60 * 30000 = 1,800,000ms at max, but let's advance in chunks
      for (let i = 0; i < 65; i++) {
        await act(async () => {
          vi.advanceTimersByTime(35000);
        });
      }

      await waitFor(() => {
        expect(screen.getByText(/taking longer than expected/i)).toBeInTheDocument();
      });
    });

    it('displays error count when job completes with errors', async () => {
      server.use(
        http.post('/api/ingestion/upload', () => {
          return HttpResponse.json(
            { jobId: 'test-job-errors' },
            { status: 202 }
          );
        }),
        http.get('/api/ingestion/jobs/:jobId', () => {
          return HttpResponse.json({
            id: 'test-job-errors',
            status: 'COMPLETED',
            fileName: 'data.csv',
            progress: 100,
            totalRows: 200,
            errorCount: 5,
            createdAt: '2024-01-01T00:00:00Z',
          });
        })
      );

      renderIngestion();

      const file = createFile('data.csv', 1024, 'text/csv');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      // Advance timer for polling
      await act(async () => {
        vi.advanceTimersByTime(4000);
      });

      await waitFor(() => {
        // The component renders "Errors: 5" in the progress info section
        expect(screen.getByText('5')).toBeInTheDocument();
        // Verify the View Errors button appears (only shown when errorCount > 0)
        expect(screen.getByRole('button', { name: /view errors/i })).toBeInTheDocument();
      });
    });

    it('shows "Upload another file" button when job reaches terminal state', async () => {
      server.use(
        http.post('/api/ingestion/upload', () => {
          return HttpResponse.json(
            { jobId: 'test-job-done' },
            { status: 202 }
          );
        }),
        http.get('/api/ingestion/jobs/:jobId', () => {
          return HttpResponse.json({
            id: 'test-job-done',
            status: 'COMPLETED',
            fileName: 'test.csv',
            progress: 100,
            totalRows: 10,
            errorCount: 0,
            createdAt: '2024-01-01T00:00:00Z',
          });
        })
      );

      renderIngestion();

      const file = createFile('test.csv', 1024, 'text/csv');
      const dropzone = screen.getByRole('button', { name: /upload file drop zone/i });

      await act(async () => {
        fireEvent.drop(dropzone, {
          dataTransfer: { files: [file] },
        });
      });

      await act(async () => {
        vi.advanceTimersByTime(4000);
      });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /upload another file/i })).toBeInTheDocument();
      });
    });
  });
});
