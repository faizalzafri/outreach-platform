import { describe, it, expect } from 'vitest';
import { truncateName, formatSharedWith, isValidDateRange, formatRelativeTime } from '../tenant-utils';
import type { ResourcePermissions } from '@/types/tenant';

describe('tenant-utils', () => {
  describe('truncateName', () => {
    it('returns the name unchanged if within maxLength', () => {
      expect(truncateName('Acme', 10)).toBe('Acme');
    });

    it('returns the name unchanged if exactly maxLength', () => {
      expect(truncateName('Acme Corp!', 10)).toBe('Acme Corp!');
    });

    it('truncates and appends an ellipsis if longer than maxLength', () => {
      expect(truncateName('Acme Corporation International', 10)).toBe('Acme Corpo…');
    });
  });

  describe('formatSharedWith', () => {
    it('formats PRIVATE visibility', () => {
      const permissions: ResourcePermissions = { visibility: 'PRIVATE', teamPermissions: [] };
      expect(formatSharedWith(permissions)).toBe('Private');
    });

    it('formats TENANT visibility regardless of team list', () => {
      const permissions: ResourcePermissions = { visibility: 'TENANT', teamPermissions: [] };
      expect(formatSharedWith(permissions)).toBe('Everyone in tenant');
    });

    it('formats TEAM visibility with no teams', () => {
      const permissions: ResourcePermissions = { visibility: 'TEAM', teamPermissions: [] };
      expect(formatSharedWith(permissions)).toBe('No teams');
    });

    it('formats TEAM visibility with up to 3 teams by name', () => {
      const permissions: ResourcePermissions = {
        visibility: 'TEAM',
        teamPermissions: [
          { teamId: '1', teamName: 'Sales', permissionLevel: 'VIEW' },
          { teamId: '2', teamName: 'Marketing', permissionLevel: 'EDIT' },
        ],
      };
      expect(formatSharedWith(permissions)).toBe('Sales, Marketing');
    });

    it('formats TEAM visibility with overflow beyond 3 teams', () => {
      const permissions: ResourcePermissions = {
        visibility: 'TEAM',
        teamPermissions: [
          { teamId: '1', teamName: 'Sales', permissionLevel: 'VIEW' },
          { teamId: '2', teamName: 'Marketing', permissionLevel: 'EDIT' },
          { teamId: '3', teamName: 'Support', permissionLevel: 'VIEW' },
          { teamId: '4', teamName: 'Engineering', permissionLevel: 'MANAGE' },
        ],
      };
      expect(formatSharedWith(permissions)).toBe('Sales, Marketing, Support +1 more');
    });
  });

  describe('isValidDateRange', () => {
    it('accepts a range where end is after start and within 90 days', () => {
      expect(isValidDateRange(new Date('2026-01-01'), new Date('2026-01-15'))).toBe(true);
    });

    it('accepts a range exactly 90 days apart', () => {
      const start = new Date('2026-01-01');
      const end = new Date(start.getTime() + 90 * 24 * 60 * 60 * 1000);
      expect(isValidDateRange(start, end)).toBe(true);
    });

    it('rejects a range longer than 90 days', () => {
      const start = new Date('2026-01-01');
      const end = new Date(start.getTime() + 91 * 24 * 60 * 60 * 1000);
      expect(isValidDateRange(start, end)).toBe(false);
    });

    it('rejects a range where end precedes start', () => {
      expect(isValidDateRange(new Date('2026-01-15'), new Date('2026-01-01'))).toBe(false);
    });
  });

  describe('formatRelativeTime', () => {
    it('formats a past timestamp as "ago"', () => {
      const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString();
      expect(formatRelativeTime(twoHoursAgo)).toBe('2 hours ago');
    });

    it('formats a future timestamp as "in"', () => {
      const inTwoDays = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString();
      expect(formatRelativeTime(inTwoDays)).toBe('in 2 days');
    });

    it('formats a very recent timestamp in seconds', () => {
      const justNow = new Date(Date.now() - 5 * 1000).toISOString();
      expect(formatRelativeTime(justNow)).toBe('5 seconds ago');
    });
  });
});
