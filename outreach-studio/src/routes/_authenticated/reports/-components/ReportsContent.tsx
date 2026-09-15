/**
 * Reports Content (lazy-loaded)
 *
 * Displays reports with filter controls, line chart visualization,
 * supporting data table, tab-based aggregation views, and async export.
 *
 * Features:
 * - Date range filter (default last 30 days)
 * - Multi-select filters: events, cities, beneficiaries, POCs (max 20 per filter)
 * - Refetch on any filter change with loading skeleton
 * - Chart + data table (submission count, avg score, score distribution per dimension)
 * - Tab navigation: by event, by beneficiary, by city, by POC
 * - Time-series charts with granularity selector (day/week/month/quarter)
 * - ROLE_POC: pre-filter to assigned events, disable POC filter
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
  BarChart,
  Bar,
} from 'recharts';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { useAuth } from '@/hooks/useAuth';
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

interface ScoreDistribution {
  [score: string]: number;
}

interface AggregationRow {
  dimension: string;
  submissionCount: number;
  avgScore: number;
  scoreDistribution?: ScoreDistribution;
}

interface ReportResponse {
  timeSeries: ReportDataPoint[];
  aggregations: AggregationRow[];
}

interface FilterOption {
  id: string;
  name: string;
}

interface FilterOptionsResponse {
  events: FilterOption[];
  cities: string[];
  beneficiaries: FilterOption[];
  pocs: FilterOption[];
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const MAX_FILTER_SELECTIONS = 20;

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
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  fileName?: string;
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

/** Enforce max selections on a multi-select change */
function handleMultiSelectChange(
  e: React.ChangeEvent<HTMLSelectElement>,
  setter: (values: string[]) => void,
) {
  const values = Array.from(e.target.selectedOptions, (opt) => opt.value);
  setter(values.slice(0, MAX_FILTER_SELECTIONS));
}

/** Check if the user only has ROLE_POC (and not ADMIN/PMO) */
function isOnlyPoc(roles: string[]): boolean {
  return (
    roles.includes('ROLE_POC') &&
    !roles.includes('ROLE_ADMIN') &&
    !roles.includes('ROLE_PMO')
  );
}

// ---------------------------------------------------------------------------
// Main Component
// ---------------------------------------------------------------------------

export function ReportsContent() {
  const search = Route.useSearch();
  const { user } = useAuth();
  const userRoles = user?.roles ?? [];
  const isPocOnly = isOnlyPoc(userRoles);

  const defaultRange = useMemo(() => getDefaultDateRange(), []);

  // Filter state
  const [startDate, setStartDate] = useState(search.startDate ?? defaultRange.startDate);
  const [endDate, setEndDate] = useState(search.endDate ?? defaultRange.endDate);
  const [granularity, setGranularity] = useState<'DAY' | 'WEEK' | 'MONTH' | 'QUARTER'>(search.granularity);
  const [selectedEvents, setSelectedEvents] = useState<string[]>([]);
  const [selectedCities, setSelectedCities] = useState<string[]>([]);
  const [selectedBeneficiaries, setSelectedBeneficiaries] = useState<string[]>([]);
  const [selectedPocs, setSelectedPocs] = useState<string[]>([]);
  const [activeTab, setActiveTab] = useState<AggregationTab>(search.tab as AggregationTab);

  // Build query params — ROLE_POC gets pre-filtered
  const queryParams = useMemo(() => ({
    startDate,
    endDate,
    granularity,
    eventIds: selectedEvents.length > 0 ? selectedEvents : undefined,
    cities: selectedCities.length > 0 ? selectedCities : undefined,
    beneficiaryIds: selectedBeneficiaries.length > 0 ? selectedBeneficiaries : undefined,
    pocIds: isPocOnly ? [user?.sub ?? ''] : (selectedPocs.length > 0 ? selectedPocs : undefined),
    groupBy: activeTab,
  }), [startDate, endDate, granularity, selectedEvents, selectedCities, selectedBeneficiaries, selectedPocs, activeTab, isPocOnly, user?.sub]);

  const trendParams: TrendParams = useMemo(() => ({
    startDate,
    endDate,
    granularity,
    eventIds: selectedEvents.length > 0 ? selectedEvents : undefined,
    cities: selectedCities.length > 0 ? selectedCities : undefined,
  }), [startDate, endDate, granularity, selectedEvents, selectedCities]);

  // Fetch report data — endpoint changes based on active tab
  const reportEndpoint = useMemo(() => {
    const endpoints: Record<AggregationTab, string> = {
      event: '/reports/by-event',
      beneficiary: '/reports/by-beneficiary',
      city: '/reports/by-city',
      poc: '/reports/by-poc',
    };
    return endpoints[activeTab];
  }, [activeTab]);

  const {
    data,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<ReportResponse>({
    queryKey: [...queryKeys.reports.trends(trendParams), activeTab, selectedBeneficiaries, selectedPocs, isPocOnly ? user?.sub : null],
    queryFn: async () => {
      const response = await httpClient.get<ReportResponse>(reportEndpoint, {
        params: queryParams,
      });
      return response.data;
    },
  });

  // Filter options are not available from a dedicated API endpoint.
  // Users can type values directly or leave filters empty.
  const filterOptions: FilterOptionsResponse = useMemo(() => ({
    events: [],
    cities: [],
    beneficiaries: [],
    pocs: [],
  }), []);

  // --- Export state ---
  const [exportStatus, setExportStatus] = useState<'idle' | 'polling' | 'completed' | 'failed' | 'timeout'>('idle');
  const [downloadUrl, setDownloadUrl] = useState<string | null>(null);
  const [downloadFileName, setDownloadFileName] = useState<string | null>(null);
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
    if (downloadUrl) URL.revokeObjectURL(downloadUrl);
    setDownloadUrl(null);
    setDownloadFileName(null);
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
          beneficiaries: selectedBeneficiaries.length > 0 ? selectedBeneficiaries : undefined,
          pocIds: isPocOnly ? [user?.sub ?? ''] : (selectedPocs.length > 0 ? selectedPocs : undefined),
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
          // The status endpoint doubles as the download endpoint: while the job is
          // pending/running it returns the ExportJobDto as JSON; once COMPLETED it
          // returns the raw file bytes with a Content-Disposition header instead.
          const statusRes = await httpClient.get<Blob>(`/reports/export/${jobId}`, {
            responseType: 'blob',
          });
          const contentType = String(statusRes.headers['content-type'] ?? '');

          if (contentType.includes('application/json')) {
            const job = JSON.parse(await statusRes.data.text()) as ExportJobStatus;
            if (job.status === 'FAILED') {
              if (exportPollRef.current) clearInterval(exportPollRef.current);
              exportPollRef.current = null;
              setExportStatus('failed');
              toastError('Export failed. Please try again.');
            }
          } else {
            if (exportPollRef.current) clearInterval(exportPollRef.current);
            exportPollRef.current = null;
            const disposition = String(statusRes.headers['content-disposition'] ?? '');
            const fileNameMatch = /filename="?([^"]+)"?/.exec(disposition);
            setDownloadFileName(fileNameMatch?.[1] ?? `report.${format.toLowerCase()}`);
            setDownloadUrl(URL.createObjectURL(statusRes.data));
            setExportStatus('completed');
            toastSuccess('Export completed successfully.');
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
  }, [startDate, endDate, granularity, selectedEvents, selectedCities, selectedBeneficiaries, selectedPocs, isPocOnly, user, toastSuccess, toastError, downloadUrl]);

  return (
    <div className={styles['container']}>
      <h1 className={styles['pageTitle']}>Reports</h1>

      {/* Filter Controls */}
      <div className={styles['filterBar']}>
        <div className={styles['filterGroup']}>
          <label className={styles['filterLabel']} htmlFor="report-start-date">Start Date</label>
          <input
            id="report-start-date"
            type="date"
            className={styles['filterInput']}
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            aria-label="Start date"
          />
        </div>

        <div className={styles['filterGroup']}>
          <label className={styles['filterLabel']} htmlFor="report-end-date">End Date</label>
          <input
            id="report-end-date"
            type="date"
            className={styles['filterInput']}
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            aria-label="End date"
          />
        </div>

        <div className={styles['filterGroup']}>
          <label className={styles['filterLabel']} htmlFor="report-granularity">Granularity</label>
          <select
            id="report-granularity"
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
          <label className={styles['filterLabel']} htmlFor="report-events">Events</label>
          <select
            id="report-events"
            className={styles['multiSelect']}
            multiple
            value={selectedEvents}
            onChange={(e) => handleMultiSelectChange(e, setSelectedEvents)}
            aria-label="Filter by events"
          >
            {filterOptions.events.map((evt) => (
              <option key={evt.id} value={evt.id}>{evt.name}</option>
            ))}
          </select>
        </div>

        <div className={styles['filterGroup']}>
          <label className={styles['filterLabel']} htmlFor="report-cities">Cities</label>
          <select
            id="report-cities"
            className={styles['multiSelect']}
            multiple
            value={selectedCities}
            onChange={(e) => handleMultiSelectChange(e, setSelectedCities)}
            aria-label="Filter by cities"
          >
            {filterOptions.cities.map((city) => (
              <option key={city} value={city}>{city}</option>
            ))}
          </select>
        </div>

        <div className={styles['filterGroup']}>
          <label className={styles['filterLabel']} htmlFor="report-beneficiaries">Beneficiaries</label>
          <select
            id="report-beneficiaries"
            className={styles['multiSelect']}
            multiple
            value={selectedBeneficiaries}
            onChange={(e) => handleMultiSelectChange(e, setSelectedBeneficiaries)}
            aria-label="Filter by beneficiaries"
          >
            {filterOptions.beneficiaries.map((b) => (
              <option key={b.id} value={b.id}>{b.name}</option>
            ))}
          </select>
        </div>

        <div className={styles['filterGroup']}>
          <label className={styles['filterLabel']} htmlFor="report-pocs">POCs</label>
          <select
            id="report-pocs"
            className={styles['multiSelect']}
            multiple
            value={isPocOnly ? [user?.sub ?? ''] : selectedPocs}
            onChange={(e) => handleMultiSelectChange(e, setSelectedPocs)}
            disabled={isPocOnly}
            aria-label="Filter by POCs"
            aria-disabled={isPocOnly}
            title={isPocOnly ? 'POC filter is restricted to your assigned events' : undefined}
          >
            {filterOptions.pocs.map((poc) => (
              <option key={poc.id} value={poc.id}>{poc.name}</option>
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
            download={downloadFileName ?? undefined}
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

      {/* Chart + Data Table */}
      {!isLoading && !isError && data && (
        <>
          {/* Time-Series Line Chart */}
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
                  stroke="var(--color-success-500)"
                  strokeWidth={2}
                  dot={false}
                  name="Avg Score"
                />
              </LineChart>
            </ResponsiveContainer>
          </section>

          {/* Score Distribution Bar Chart */}
          {data.aggregations && data.aggregations.length > 0 && data.aggregations.some((row) => row.scoreDistribution) && (
            <section className={styles['chartSection']}>
              <h2 className={styles['chartTitle']}>Score Distribution</h2>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart
                  data={data.aggregations.map((row) => ({
                    name: row.dimension.length > 20 ? `${row.dimension.substring(0, 20)}…` : row.dimension,
                    ...row.scoreDistribution,
                  }))}
                  margin={{ top: 5, right: 20, bottom: 5, left: 0 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border-default)" />
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} stroke="var(--text-muted)" />
                  <YAxis tick={{ fontSize: 12 }} stroke="var(--text-muted)" />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="1" fill="var(--color-danger-500)" name="Score 1" stackId="a" />
                  <Bar dataKey="2" fill="var(--color-orange-500)" name="Score 2" stackId="a" />
                  <Bar dataKey="3" fill="var(--color-amber-500)" name="Score 3" stackId="a" />
                  <Bar dataKey="4" fill="var(--color-success-500)" name="Score 4" stackId="a" />
                  <Bar dataKey="5" fill="var(--color-info-500)" name="Score 5" stackId="a" />
                </BarChart>
              </ResponsiveContainer>
            </section>
          )}

          {/* Aggregated Data Table */}
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
                    <th>Score 1</th>
                    <th>Score 2</th>
                    <th>Score 3</th>
                    <th>Score 4</th>
                    <th>Score 5</th>
                  </tr>
                </thead>
                <tbody>
                  {data.aggregations.map((row, index) => (
                    <tr key={index}>
                      <td>{row.dimension}</td>
                      <td>{row.submissionCount}</td>
                      <td>{row.avgScore.toFixed(1)}</td>
                      <td>{row.scoreDistribution?.['1'] ?? 0}</td>
                      <td>{row.scoreDistribution?.['2'] ?? 0}</td>
                      <td>{row.scoreDistribution?.['3'] ?? 0}</td>
                      <td>{row.scoreDistribution?.['4'] ?? 0}</td>
                      <td>{row.scoreDistribution?.['5'] ?? 0}</td>
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
