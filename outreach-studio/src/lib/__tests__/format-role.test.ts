import { describe, it, expect } from 'vitest';
import { formatRole } from '../format-role';

describe('formatRole', () => {
  it('keeps acronyms and turns longer roles into words', () => {
    expect(formatRole('ROLE_PMO')).toBe('PMO');
    expect(formatRole('ROLE_POC')).toBe('POC');
    expect(formatRole('ROLE_ADMIN')).toBe('Admin');
    expect(formatRole('ROLE_PLATFORM_ADMIN')).toBe('Platform admin');
  });
});
