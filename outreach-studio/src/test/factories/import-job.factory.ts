import type { ImportJob } from '@/types/domain';

let counter = 0;

export function buildImportJob(overrides: Partial<ImportJob> = {}): ImportJob {
  counter += 1;
  return {
    id: `import-${counter}`,
    fileName: `volunteers_batch_${counter}.csv`,
    status: 'PENDING',
    progress: 0,
    totalRows: 100,
    errorCount: 0,
    createdAt: '2025-03-01T08:00:00Z',
    ...overrides,
  };
}
