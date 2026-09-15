import type { ResourcePermissions } from '@/types/tenant';

const MAX_DATE_RANGE_DAYS = 90;
const MS_PER_DAY = 24 * 60 * 60 * 1000;

/** Truncates a name to maxLength characters, appending an ellipsis if it was cut. */
export function truncateName(name: string, maxLength: number): string {
  if (name.length <= maxLength) {
    return name;
  }
  return `${name.slice(0, maxLength)}…`;
}

/** Formats the "Shared with" column for a resource list: visibility-appropriate summary text. */
export function formatSharedWith(permissions: ResourcePermissions): string {
  if (permissions.visibility === 'PRIVATE') {
    return 'Private';
  }
  if (permissions.visibility === 'TENANT') {
    return 'Everyone in tenant';
  }

  const { teamPermissions } = permissions;
  if (teamPermissions.length === 0) {
    return 'No teams';
  }
  if (teamPermissions.length <= 3) {
    return teamPermissions.map((p) => p.teamName).join(', ');
  }

  const shown = teamPermissions.slice(0, 3).map((p) => p.teamName).join(', ');
  const overflow = teamPermissions.length - 3;
  return `${shown} +${overflow} more`;
}

/** Validates an activity feed date range: end must not precede start, and span must not exceed 90 days. */
export function isValidDateRange(start: Date, end: Date): boolean {
  if (end < start) {
    return false;
  }
  const spanDays = (end.getTime() - start.getTime()) / MS_PER_DAY;
  return spanDays <= MAX_DATE_RANGE_DAYS;
}

const RELATIVE_TIME_FORMATTER = new Intl.RelativeTimeFormat('en', { numeric: 'auto' });

const RELATIVE_TIME_UNITS: Array<{ unit: Intl.RelativeTimeFormatUnit; seconds: number }> = [
  { unit: 'year', seconds: 31536000 },
  { unit: 'month', seconds: 2592000 },
  { unit: 'week', seconds: 604800 },
  { unit: 'day', seconds: 86400 },
  { unit: 'hour', seconds: 3600 },
  { unit: 'minute', seconds: 60 },
];

/** Formats an ISO timestamp as human-readable relative time (e.g. "3 hours ago", "in 2 days"). */
export function formatRelativeTime(isoTimestamp: string): string {
  const then = new Date(isoTimestamp).getTime();
  const now = Date.now();
  const diffSeconds = (then - now) / 1000;

  for (const { unit, seconds } of RELATIVE_TIME_UNITS) {
    if (Math.abs(diffSeconds) >= seconds) {
      return RELATIVE_TIME_FORMATTER.format(Math.round(diffSeconds / seconds), unit);
    }
  }

  return RELATIVE_TIME_FORMATTER.format(Math.round(diffSeconds), 'second');
}
