/**
 * Feedback List Content (lazy-loaded)
 *
 * Read-only DataTable view for ADMIN and PMO users showing all submitted
 * volunteer feedback with filtering, sorting, and pagination.
 */

import { useMemo } from 'react';
import type { ColumnDef } from '@tanstack/react-table';

import { DataTable } from '@/components/data-table/DataTable';
import { queryKeys } from '@/lib/query-keys';

import styles from './FeedbackListContent.module.css';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface FeedbackRecord {
  id: string;
  eventName: string;
  volunteerId: string;
  score: number;
  category: string;
  sentiment: string | null;
  status: string;
  submittedAt: string;
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const EMOJI_LABELS = ['😞', '😕', '😐', '🙂', '😄'];

const CATEGORY_OPTIONS = [
  { label: 'Communication', value: 'Communication' },
  { label: 'Organization', value: 'Organization' },
  { label: 'Content', value: 'Content' },
  { label: 'Logistics', value: 'Logistics' },
  { label: 'Overall', value: 'Overall' },
];

const SENTIMENT_OPTIONS = [
  { label: 'Positive', value: 'POSITIVE' },
  { label: 'Neutral', value: 'NEUTRAL' },
  { label: 'Negative', value: 'NEGATIVE' },
];

const STATUS_OPTIONS = [
  { label: 'Submitted', value: 'SUBMITTED' },
  { label: 'Reviewed', value: 'REVIEWED' },
  { label: 'Flagged', value: 'FLAGGED' },
  { label: 'Archived', value: 'ARCHIVED' },
];

// ---------------------------------------------------------------------------
// Badge components
// ---------------------------------------------------------------------------

function ScoreBadge({ score }: { score: number }) {
  const emoji = EMOJI_LABELS[score - 1] ?? '—';
  return (
    <span className={styles['scoreBadge']}>
      {emoji} {score}/5
    </span>
  );
}

function SentimentBadge({ sentiment }: { sentiment: string | null }) {
  if (!sentiment) return <span>—</span>;
  const variant = sentiment as 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';
  return (
    <span className={`${styles['sentimentBadge']} ${styles[`sentimentBadge--${variant}`]}`}>
      {sentiment}
    </span>
  );
}

function StatusBadge({ status }: { status: string }) {
  const variant = status as 'SUBMITTED' | 'REVIEWED' | 'FLAGGED' | 'ARCHIVED';
  return (
    <span className={`${styles['statusBadge']} ${styles[`statusBadge--${variant}`]}`}>
      {status}
    </span>
  );
}

// ---------------------------------------------------------------------------
// Column definitions
// ---------------------------------------------------------------------------

const columns: ColumnDef<FeedbackRecord, unknown>[] = [
  {
    accessorKey: 'eventName',
    header: 'Event Name',
    enableSorting: true,
    enableColumnFilter: true,
    meta: {
      filterType: 'text',
    },
  },
  {
    accessorKey: 'volunteerId',
    header: 'Volunteer ID',
    enableSorting: true,
    enableColumnFilter: false,
  },
  {
    accessorKey: 'score',
    header: 'Score',
    enableSorting: true,
    enableColumnFilter: false,
    cell: ({ getValue }) => <ScoreBadge score={getValue() as number} />,
  },
  {
    accessorKey: 'category',
    header: 'Category',
    enableSorting: true,
    enableColumnFilter: true,
    meta: {
      filterType: 'select',
      filterOptions: CATEGORY_OPTIONS,
    },
  },
  {
    accessorKey: 'sentiment',
    header: 'Sentiment',
    enableSorting: true,
    enableColumnFilter: true,
    meta: {
      filterType: 'select',
      filterOptions: SENTIMENT_OPTIONS,
    },
    cell: ({ getValue }) => <SentimentBadge sentiment={getValue() as string | null} />,
  },
  {
    accessorKey: 'status',
    header: 'Status',
    enableSorting: true,
    enableColumnFilter: true,
    meta: {
      filterType: 'select',
      filterOptions: STATUS_OPTIONS,
    },
    cell: ({ getValue }) => <StatusBadge status={getValue() as string} />,
  },
  {
    accessorKey: 'submittedAt',
    header: 'Submitted At',
    enableSorting: true,
    enableColumnFilter: false,
    cell: ({ getValue }) => {
      const dateStr = getValue() as string;
      if (!dateStr) return '—';
      return new Date(dateStr).toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    },
  },
];

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function FeedbackListContent() {
  const queryKey = useMemo(() => queryKeys.feedback.lists(), []);

  return (
    <div className={styles['container']}>
      <div className={styles['header']}>
        <h1 className={styles['pageTitle']}>Feedback</h1>
      </div>

      <DataTable<FeedbackRecord>
        columns={columns}
        queryKey={queryKey}
        endpoint="/feedback"
        defaultPageSize={10}
        searchPlaceholder="Search feedback..."
        enableColumnVisibility={true}
        emptyMessage="No feedback records found."
        caption="Submitted volunteer feedback listing"
      />
    </div>
  );
}
