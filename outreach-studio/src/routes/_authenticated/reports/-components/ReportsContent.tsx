/**
 * Reports Content (lazy-loaded)
 *
 * Displays reports with filter controls, line chart visualization,
 * supporting data table, tab-based aggregation views, and async export.
 */

import { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useToast } from '@/hooks/useToast';
import type { TrendParams } from '@/types/api';

import { Route } from '../index';
import styles from './ReportsContent.module.css';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type AggregationTab = 'event' | 'beneficiary' | 'city' | 'poc';

interface ReportDataPoint {
  date: string;
  count: number;
  avgScore?: number;
}

interface AggregationRow {
  dimension: string;
  submissionCount: number;
  avgScore: number;
  scoreDistribution?: Record<string, number>;
}

interface ReportResponse {
  timeSeries: ReportDataPoint[];
  aggregations: AggregationRow[];
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const GRANULARITY_OPTIONS = [
  { value: 'DAY', label: 'Day' },
  { value: 'WEEK', label: 'Week' },
  { value: 'MONTH', label: 'Month' },
  { value: 'QUARTER', label: 'Quarter' },
] as const;

const TAB_LABELS: Record<AggregationTab, string> = {
  event: 'By Event',
  beneficiary: 'By Beneficiary',
  city: 'By City',
  poc: 'By POC',
};

// ---------------------------------------------------------------------------
// Export types
// ---------------------------------------------------------------------------

type ExportFormat = 'PDF' | 'CSV' | 'EXCEL';

interface ExportJobStatus {
  jobId: string;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  downloadUrl?: string;
}

const EXPORT_FORMAT_OPTIONS: { value: ExportFormat; label: string }[] = [
  { value: 'PDF', label: 'PDF' },
  { value: 'CSV', label: 'CSV' },
  { value: 'EXCEL', label: 'Excel' },
];

const EXPORT_POLL_INTERVAL = 5_000; // 5 seconds
const EXPORT_MAX_DURATION = 120_000; // 2 minutes

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function getDefaultDateRange() {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 30);
  return {
    startDate: start.toISOString().split('T')[0]!,
    endDate: end.toISOString().split('T')[0]!,
  };
}

function formatDateForChart(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`;
}

// ---------------------------------------------------------------------------
// Main Component
// ---------------------------------------------------------------------------

export function ReportsContent() {
  const search = Route.useSearch();

  const defaultRange = useMemo(() => getDefaultDateRange(), []);

  // Filter state
  const [startDate, setStartDate] = useState(search.startDate ?? defaultRange.startDate);
  const [endDate, setEndDate] = useState(search.endDate ?? defaultRange.endDate);
  const [granularity, setGranularity] = useState<'DAY' | 'WEEK' | 'MONTH' | 'QUARTER'>(search.granularity);
  const [selectedEvents, setSelectedEvents] = useState<string[]>([]);
  const [selectedCities, setSelectedCities] = useState<string[]>([]);
  const [activeTab, setActiveTab] = useState<AggregationTab>(search.tab as AggregationTab);

  // Build query params
  const queryParams = useMemo(() => ({
    startDate,
    endDate,
    granularity,
    eventIds: selectedEvents.length > 0 ? selectedEvents : undefined,
    cities: selectedCities.length > 0 ? selectedCities : undefined,
    groupBy: activeTab,
  }), [startDate, endDate, granularity, selectedEvents, selectedCities, activeTab]);

  const trendParams: TrendParams = useMemo(() => ({
    startDate,
    endDate,
    granularity,
    eventIds: selectedEvents.length > 0 ? selectedEvents : undefined,
    cities: selectedCities.length > 0 ? selectedCities : undefined,
  }), [startDate, endDate, granularity, selectedEvents, selectedCities]);

  // Fetch report data
  const {
    data,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<ReportResponse>({
    queryKey: [...queryKeys.reports.trends(trendParams), activeTab],
    queryFn: async () => {
      const response = await httpClient.get<ReportResponse>('/reports/feedback', {
        params: queryParams,
      });
      return response.data;
    },
  });

  // Fetch available events and cities for filter options
  const { data: filterOptions } = useQuery<{ events: Array<{ id: string; name: string }>; cities: string[] }>({
    queryKey: ['reports', 'filter-options'],
    queryFn: async () => {
      const response = await httpClient.get<{ events: Array<{ id: string; name: string }>; cities: string[] }>(
        '/reports/filter-options'
      );
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // cache 5 min
  });

  const handleEventsChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const values = Array.from(e.target.selectedOptions, (opt) => opt.value);
    setSelectedEvents(values);
  };

  const handleCitiesChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const values = Array.from(e.target.selectedOptions, (opt) => opt.value);
    setSelectedCities(values);
  };

  // --- Export state ---
  const [exportStatus, setExportStatus] = useState<'idle' | 'polling' | 'completed' | 'failed' | 'timeout'>('idle');
  const [downloadUrl, setDownloadUrl] = useState<string | null>(null);
  const [showFormatMenu, setShowFormatMenu] = useState(false);
  const exportPollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const exportStartRef = useRef<number>(0);
  const { success: toastSuccess, error: toastError } = useToast();

  // Cleanup poll on unmount
  useEffect(() => {
    return () => {
      if (exportPollRef.current) {
        clearInterval(exportPollRef.current);
      }
    };
  }, []);

  const startExport = useCallback(async (format: ExportFormat) => {
    setShowFormatMenu(false);
    setExportStatus('polling');
    setDownloadUrl(null);
    exportStartRef.current = Date.now();

    try {
      const response = await httpClient.post<{ jobId: string }>('/reports/export', {
        format,
        filters: {
          startDate,
          endDate,
          granularity,
          eventIds: selectedEvents.length > 0 ? selectedEvents : undefined,
          cities: selectedCities.length > 0 ? selectedCities : undefined,
        },
      });

      const jobId = response.data.jobId;

      // Start polling
      exportPollRef.current = setInterval(async () => {
        const elapsed = Date.now() - exportStartRef.current;
        if (elapsed >= EXPORT_MAX_DURATION) {
          if (exportPollRef.current) clearInterval(exportPollRef.current);
          exportPollRef.current = null;
          setExportStatus('timeout');
          toastError('Export timed out. Please try again.');
          return;
        }

        try {
          const statusRes = await httpClient.get<ExportJobStatus>(`/reports/export/${jobId}/status`);
          const job = statusRes.data;

          if (job.status === 'COMPLETED') {
            if (exportPollRef.current) clearInterval(exportPollRef.current);
            exportPollRef.current = null;
            setExportStatus('completed');
            setDownloadUrl(job.downloadUrl ?? null);
            toastSuccess('Export completed successfully.');
          } else if (job.status === 'FAILED') {
            if (exportPollRef.current) clearInterval(exportPollRef.current);
            exportPollRef.current = null;
            setExportStatus('failed');
            toastError('Export failed. Please try again.');
          }
        } catch {
          if (exportPollRef.current) clearInterval(exportPollRef.current);
          exportPollRef.current = null;
          setExportStatus('failed');
          toastError('Failed to check export status.');
        }
      }, EXPORT_POLL_INTERVAL);
    } catch {
      setExportStatus('failed');
      toastError('Failed to start export.');
    }
  }, [startDate, endDate, granularity, selectedEvents, selectedCities, toastSuccess, toastError]);

  return (
    <div className={styles['container']}>
      <h1 className={styles['pageTitle']}>Reports</h1>

      {/* Filter Controls */}
      <div className={styles['filterBar']}>
        <div className={styles['filterGroup']}>
          <span className={styles['filterLabel']}>Start Date</span>
          <input
            type="date"
            className={styles['filterInput']}
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            aria-label="Start date"
          />
        </div>

        <div className={styles['filterGroup']}>
          <span className={styles['filterLabel']}>End Date</span>
          <input
            type="date"
            className={styles['filterInput']}
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            aria-label="End date"
          />
        </div>

        <div className={styles['filterGroup']}>
          <span className={styles['filterLabel']}>Granularity</span>
          <select
            className={styles['filterSelect']}
            value={granularity}
            onChange={(e) => setGranularity(e.target.value as typeof granularity)}
            aria-label="Granularity"
          >
            {GRANULARITY_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>

        <div className={styles['filterGroup']}>
          <span className={styles['filterLabel']}>Events</span>
          <select
            className={styles['multiSelect']}
            multiple
            value={selectedEvents}
            onChange={handleEventsChange}
            aria-label="Filter by events"
          >
            {filterOptions?.events?.map((evt) => (
              <option key={evt.id} value={evt.id}>{evt.name}</option>
            ))}
          </select>
        </div>

        <div className={styles['filterGroup']}>
          <span className={styles['filterLabel']}>Cities</span>
          <select
            className={styles['multiSelect']}
            multiple
            value={selectedCities}
            onChange={handleCitiesChange}
            aria-label="Filter by cities"
          >
            {filterOptions?.cities?.map((city) => (
              <option key={city} value={city}>{city}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Export Controls */}
      <div className={styles['exportBar']}>
        <div className={styles['exportGroup']}>
          <button
            type="button"
            className={styles['exportBtn']}
            disabled={exportStatus === 'polling'}
            onClick={() => setShowFormatMenu((prev) => !prev)}
            aria-haspopup="true"
            aria-expanded={showFormatMenu}
          >
            {exportStatus === 'polling' ? (
              <>
                <span className={styles['spinner']} aria-hidden="true" />
                Exporting…
              </>
            ) : (
              'Export'
            )}
          </button>
          {showFormatMenu && (
            <div className={styles['formatMenu']} role="menu">
              {EXPORT_FORMAT_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  role="menuitem"
                  className={styles['formatMenuItem']}
                  onClick={() => void startExport(opt.value)}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          )}
        </div>
        {exportStatus === 'completed' && downloadUrl && (
          <a
            href={downloadUrl}
            className={styles['downloadLink']}
            target="_blank"
            rel="noopener noreferrer"
            download
          >
            Download Report
          </a>
        )}
        {exportStatus === 'failed' && (
          <span className={styles['exportError']}>Export failed. Try again.</span>
        )}
        {exportStatus === 'timeout' && (
          <span className={styles['exportError']}>Export timed out.</span>
        )}
      </div>

      {/* Tab Navigation */}
      <div className={styles['tabs']} role="tablist" aria-label="Report aggregation views">
        {(Object.entries(TAB_LABELS) as [AggregationTab, string][]).map(([tab, label]) => (
          <button
            key={tab}
            type="button"
            role="tab"
            className={`${styles['tab']} ${activeTab === tab ? styles['tabActive'] : ''}`}
            aria-selected={activeTab === tab}
            onClick={() => setActiveTab(tab)}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Loading Skeleton */}
      {isLoading && (
        <>
          <div className={`${styles['skeleton']} ${styles['chartSkeleton']}`} role="status" aria-label="Loading chart" />
          <div className={`${styles['skeleton']} ${styles['tableSkeleton']}`} role="status" aria-label="Loading data" />
        </>
      )}

      {/* Error State */}
      {isError && !isLoading && (
        <div className={styles['errorState']}>
          <p className={styles['errorMessage']}>
            {(error as { message?: string })?.message ?? 'Failed to load report data'}
          </p>
          <button type="button" className={styles['retryBtn']} onClick={() => void refetch()}>
            Retry
          </button>
        </div>
      )}

      {/* Chart */}
      {!isLoading && !isError && data && (
        <>
          <section className={styles['chartSection']}>
            <h2 className={styles['chartTitle']}>Feedback Trends</h2>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={data.timeSeries} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-default)" />
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 12 }}
                  tickFormatter={formatDateForChart}
                  stroke="var(--text-muted)"
                />
                <YAxis tick={{ fontSize: 12 }} stroke="var(--text-muted)" />
                <Tooltip />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="count"
                  stroke="var(--accent)"
                  strokeWidth={2}
                  dot={false}
                  name="Submissions"
                />
                <Line
                  type="monotone"
                  dataKey="avgScore"
                  stroke="#22c55e"
                  strokeWidth={2}
                  dot={false}
                  name="Avg Score"
                />
              </LineChart>
            </ResponsiveContainer>
          </section>

          {/* Data Table */}
          <section className={styles['tableSection']}>
            <h2 className={styles['sectionTitle']}>
              Aggregated Data — {TAB_LABELS[activeTab]}
            </h2>
            {data.aggregations && data.aggregations.length > 0 ? (
              <table className={styles['dataTable']}>
                <thead>
                  <tr>
                    <th>{TAB_LABELS[activeTab].replace('By ', '')}</th>
                    <th>Submissions</th>
                    <th>Avg Score</th>
                  </tr>
                </thead>
                <tbody>
                  {data.aggregations.map((row, index) => (
                    <tr key={index}>
                      <td>{row.dimension}</td>
                      <td>{row.submissionCount}</td>
                      <td>{row.avgScore.toFixed(1)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                No data available for the selected filters.
              </p>
            )}
          </section>
        </>
      )}
    </div>
  );
}
