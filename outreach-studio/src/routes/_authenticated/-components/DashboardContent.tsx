/**
 * Dashboard Content (lazy-loaded)
 *
 * Renders KPI cards, feedback trends line chart, event status bar chart,
 * and feedback score distribution pie chart. Data is fetched via parallel
 * TanStack Query calls. Each widget handles its own loading/error state
 * independently so a single failure doesn't take down the whole page.
 *
 * Access restricted to ROLE_ADMIN and ROLE_PMO.
 */

import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';

import { httpClient } from '@/lib/http-client';
import { queryKeys } from '@/lib/query-keys';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import type { DashboardKPIs } from '@/types/domain';
import type { TrendParams } from '@/types/api';

import styles from './DashboardContent.module.css';

// ---------------------------------------------------------------------------
// Types for API responses
// ---------------------------------------------------------------------------

interface TrendDataPoint {
  date: string;
  count: number;
  avgScore?: number;
}

interface StatusDistribution {
  status: string;
  count: number;
}

interface ScoreDistribution {
  score: number;
  count: number;
}

interface TrendsResponse {
  feedbackTrends: TrendDataPoint[];
  eventStatusDistribution: StatusDistribution[];
  feedbackScoreDistribution: ScoreDistribution[];
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const PIE_COLORS = ['#ef4444', '#f59e0b', '#eab308', '#22c55e', '#6366f1'];

const REQUIRED_ROLES = ['ROLE_ADMIN', 'ROLE_PMO'];

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

function formatDateForDisplay(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`;
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

function KpiCardSkeleton() {
  return <div className={`${styles['skeleton']} ${styles['kpiSkeleton']}`} />;
}

function ChartSkeleton() {
  return <div className={`${styles['skeleton']} ${styles['chartSkeleton']}`} />;
}

interface ErrorWidgetProps {
  message: string;
  onRetry: () => void;
}

function ErrorWidget({ message, onRetry }: ErrorWidgetProps) {
  return (
    <div className={styles['errorState']}>
      <p className={styles['errorMessage']}>{message}</p>
      <button type="button" className={styles['retryBtn']} onClick={onRetry}>
        Retry
      </button>
    </div>
  );
}

// ---------------------------------------------------------------------------
// KPI Cards
// ---------------------------------------------------------------------------

interface KpiItem {
  label: string;
  value: string;
}

function KpiCards() {
  const { data, isLoading, isError, error, refetch } = useQuery<DashboardKPIs>({
    queryKey: queryKeys.reports.dashboard({}),
    queryFn: async () => {
      const response = await httpClient.get<DashboardKPIs>('/reports/dashboard/kpis');
      return response.data;
    },
  });

  if (isLoading) {
    return (
      <div className={styles['kpiGrid']} role="status" aria-label="Loading KPIs">
        {Array.from({ length: 6 }).map((_, i) => (
          <KpiCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className={styles['kpiGrid']}>
        <div className={styles['chartCard']} style={{ gridColumn: '1 / -1' }}>
          <ErrorWidget
            message={(error as { message?: string })?.message ?? 'Failed to load KPIs'}
            onRetry={() => void refetch()}
          />
        </div>
      </div>
    );
  }

  const kpis: KpiItem[] = [
    { label: 'Total Events', value: String(data.totalEvents ?? 0) },
    { label: 'Active Events', value: String(data.activeEvents ?? 0) },
    { label: 'Total Volunteers', value: String(data.totalVolunteers ?? 0) },
    { label: 'Avg Feedback Score', value: (data.averageFeedbackScore ?? 0).toFixed(1) },
    { label: 'Pending Feedback', value: String(data.pendingFeedback ?? 0) },
    { label: 'Notification Delivery Rate', value: `${(data.notificationDeliveryRate ?? 0).toFixed(1)}%` },
  ];

  return (
    <div className={styles['kpiGrid']}>
      {kpis.map((kpi) => (
        <div key={kpi.label} className={styles['kpiCard']}>
          <p className={styles['kpiLabel']}>{kpi.label}</p>
          <p className={styles['kpiValue']}>{kpi.value}</p>
        </div>
      ))}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Feedback Trends Line Chart
// ---------------------------------------------------------------------------

function FeedbackTrendsChart({
  data,
  isLoading,
  isError,
  error,
  onRetry,
  startDate,
  endDate,
  granularity,
  onStartDateChange,
  onEndDateChange,
  onGranularityChange,
}: {
  data: TrendDataPoint[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  onRetry: () => void;
  startDate: string;
  endDate: string;
  granularity: 'DAY' | 'WEEK' | 'MONTH';
  onStartDateChange: (val: string) => void;
  onEndDateChange: (val: string) => void;
  onGranularityChange: (val: 'DAY' | 'WEEK' | 'MONTH') => void;
}) {
  return (
    <div className={`${styles['chartCard']} ${styles['chartCardFull']}`}>
      <h2 className={styles['chartTitle']}>Feedback Trends</h2>
      <div className={styles['chartControls']}>
        <label>
          <span className="sr-only">Start date</span>
          <input
            type="date"
            className={styles['dateInput']}
            value={startDate}
            onChange={(e) => onStartDateChange(e.target.value)}
            aria-label="Start date"
          />
        </label>
        <label>
          <span className="sr-only">End date</span>
          <input
            type="date"
            className={styles['dateInput']}
            value={endDate}
            onChange={(e) => onEndDateChange(e.target.value)}
            aria-label="End date"
          />
        </label>
        <select
          className={styles['chartSelect']}
          value={granularity}
          onChange={(e) => onGranularityChange(e.target.value as 'DAY' | 'WEEK' | 'MONTH')}
          aria-label="Granularity"
        >
          <option value="DAY">Day</option>
          <option value="WEEK">Week</option>
          <option value="MONTH">Month</option>
        </select>
      </div>

      {isLoading && <ChartSkeleton />}
      {isError && (
        <ErrorWidget
          message={(error as { message?: string })?.message ?? 'Failed to load trends'}
          onRetry={onRetry}
        />
      )}
      {!isLoading && !isError && data && (
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={data} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border-default)" />
            <XAxis
              dataKey="date"
              tick={{ fontSize: 12 }}
              tickFormatter={formatDateForDisplay}
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
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Event Status Bar Chart
// ---------------------------------------------------------------------------

function EventStatusChart({
  data,
  isLoading,
  isError,
  error,
  onRetry,
}: {
  data: StatusDistribution[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  onRetry: () => void;
}) {
  return (
    <div className={styles['chartCard']}>
      <h2 className={styles['chartTitle']}>Event Status Distribution</h2>
      {isLoading && <ChartSkeleton />}
      {isError && (
        <ErrorWidget
          message={(error as { message?: string })?.message ?? 'Failed to load status data'}
          onRetry={onRetry}
        />
      )}
      {!isLoading && !isError && data && (
        <ResponsiveContainer width="100%" height={280}>
          <BarChart data={data} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border-default)" />
            <XAxis dataKey="status" tick={{ fontSize: 11 }} stroke="var(--text-muted)" />
            <YAxis tick={{ fontSize: 12 }} stroke="var(--text-muted)" />
            <Tooltip />
            <Bar dataKey="count" fill="var(--accent)" radius={[4, 4, 0, 0]} name="Events" />
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Feedback Score Pie Chart
// ---------------------------------------------------------------------------

function FeedbackScoreChart({
  data,
  isLoading,
  isError,
  error,
  onRetry,
}: {
  data: ScoreDistribution[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  onRetry: () => void;
}) {
  return (
    <div className={styles['chartCard']}>
      <h2 className={styles['chartTitle']}>Feedback Score Distribution</h2>
      {isLoading && <ChartSkeleton />}
      {isError && (
        <ErrorWidget
          message={(error as { message?: string })?.message ?? 'Failed to load score data'}
          onRetry={onRetry}
        />
      )}
      {!isLoading && !isError && data && (
        <ResponsiveContainer width="100%" height={280}>
          <PieChart>
            <Pie
              data={data}
              dataKey="count"
              nameKey="score"
              cx="50%"
              cy="50%"
              outerRadius={100}
              label={(props) => {
                const payload = props.payload as ScoreDistribution | undefined;
                const percent = (props.percent as number) ?? 0;
                const score = payload?.score ?? 0;
                return `Score ${String(score)}: ${(percent * 100).toFixed(0)}%`;
              }}
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${String(entry.score)}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Main Dashboard Content
// ---------------------------------------------------------------------------

export function DashboardContent() {
  const defaultRange = useMemo(() => getDefaultDateRange(), []);

  const [startDate, setStartDate] = useState(defaultRange.startDate);
  const [endDate, setEndDate] = useState(defaultRange.endDate);
  const [granularity, setGranularity] = useState<'DAY' | 'WEEK' | 'MONTH'>('DAY');

  const trendParams: TrendParams = useMemo(
    () => ({
      startDate,
      endDate,
      granularity,
    }),
    [startDate, endDate, granularity],
  );

  const {
    data: trendsData,
    isLoading: trendsLoading,
    isError: trendsError,
    error: trendsErrorObj,
    refetch: refetchTrends,
  } = useQuery<TrendsResponse>({
    queryKey: queryKeys.reports.trends(trendParams),
    queryFn: async () => {
      const response = await httpClient.get<TrendsResponse>('/reports/dashboard/trends', {
        params: {
          startDate,
          endDate,
          granularity: granularity.toLowerCase(),
        },
      });
      return response.data;
    },
  });

  return (
    <ProtectedRoute requiredRoles={REQUIRED_ROLES}>
      <div className={styles['container']}>
        <h1 className={styles['pageTitle']}>Dashboard</h1>

        {/* KPI Cards — independent query */}
        <KpiCards />

        {/* Charts */}
        <div className={styles['chartGrid']}>
          {/* Feedback Trends Line Chart (full width) */}
          <FeedbackTrendsChart
            data={trendsData?.feedbackTrends}
            isLoading={trendsLoading}
            isError={trendsError}
            error={trendsErrorObj}
            onRetry={() => void refetchTrends()}
            startDate={startDate}
            endDate={endDate}
            granularity={granularity}
            onStartDateChange={setStartDate}
            onEndDateChange={setEndDate}
            onGranularityChange={setGranularity}
          />

          {/* Event Status Bar Chart */}
          <EventStatusChart
            data={trendsData?.eventStatusDistribution}
            isLoading={trendsLoading}
            isError={trendsError}
            error={trendsErrorObj}
            onRetry={() => void refetchTrends()}
          />

          {/* Feedback Score Pie Chart */}
          <FeedbackScoreChart
            data={trendsData?.feedbackScoreDistribution}
            isLoading={trendsLoading}
            isError={trendsError}
            error={trendsErrorObj}
            onRetry={() => void refetchTrends()}
          />
        </div>
      </div>
    </ProtectedRoute>
  );
}
