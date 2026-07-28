import { describe, it, expect, vi } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import type { ColumnDef } from '@tanstack/react-table';

import { renderWithProviders } from '@/test/utils';
import { server } from '@/test/server';
import { DataTable } from '../DataTable';
import type { PageResponse } from '@/types/api';

// ---------------------------------------------------------------------------
// Test data types and helpers
// ---------------------------------------------------------------------------

interface TestItem {
  id: string;
  name: string;
  status: string;
  city: string;
}

const testColumns: ColumnDef<TestItem, unknown>[] = [
  { accessorKey: 'id', header: 'ID', enableSorting: true },
  { accessorKey: 'name', header: 'Name', enableSorting: true },
  {
    accessorKey: 'status',
    header: 'Status',
    enableSorting: true,
    meta: {
      filterType: 'select',
      filterOptions: [
        { label: 'Active', value: 'ACTIVE' },
        { label: 'Draft', value: 'DRAFT' },
      ],
    },
  },
  { accessorKey: 'city', header: 'City', enableSorting: true },
];

function generateItems(count: number): TestItem[] {
  return Array.from({ length: count }, (_, i) => ({
    id: `item-${String(i + 1).padStart(3, '0')}`,
    name: `Item ${i + 1}`,
    status: i % 2 === 0 ? 'ACTIVE' : 'DRAFT',
    city: ['Mumbai', 'Delhi', 'Bangalore'][i % 3]!,
  }));
}

function createPageResponse(
  items: TestItem[],
  page: number,
  size: number,
  totalElements: number,
): PageResponse<TestItem> {
  return {
    content: items,
    totalElements,
    totalPages: Math.ceil(totalElements / size),
    page,
    size,
  };
}

/**
 * The DataTable component endpoint prop (without /api prefix).
 * httpClient has baseURL '/api', so final requests go to '/api/test-items'.
 */
const COMPONENT_ENDPOINT = '/test-items';

/** The full URL that MSW intercepts (baseURL + endpoint). */
const MSW_URL = '/api/test-items';

const TEST_QUERY_KEY = ['test-items'];

/**
 * Sets up an MSW handler for the test endpoint.
 */
function setupMswHandler(
  responseFactory: (url: URL) => PageResponse<TestItem>,
) {
  server.use(
    http.get(MSW_URL, ({ request }) => {
      const url = new URL(request.url);
      return HttpResponse.json(responseFactory(url));
    }),
  );
}

function renderDataTable(props?: Partial<React.ComponentProps<typeof DataTable<TestItem>>>) {
  return renderWithProviders(
    <DataTable<TestItem>
      columns={testColumns}
      queryKey={TEST_QUERY_KEY}
      endpoint={COMPONENT_ENDPOINT}
      defaultPageSize={10}
      caption="Test items table"
      {...props}
    />,
  );
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('DataTable', () => {
  describe('Pagination controls and page navigation', () => {
    it('renders pagination info with total records and page numbers', async () => {
      const allItems = generateItems(25);
      setupMswHandler((url) => {
        const page = Number(url.searchParams.get('page') ?? '0');
        const size = Number(url.searchParams.get('size') ?? '10');
        const slice = allItems.slice(page * size, (page + 1) * size);
        return createPageResponse(slice, page, size, 25);
      });

      renderDataTable();

      await waitFor(() => {
        expect(screen.getByText(/25 total records/)).toBeInTheDocument();
      });
      expect(screen.getByText(/Page 1 of 3/)).toBeInTheDocument();
    });

    it('disables First and Previous buttons on first page', async () => {
      setupMswHandler(() =>
        createPageResponse(generateItems(10), 0, 10, 25),
      );

      renderDataTable();

      await waitFor(() => {
        expect(screen.getByText(/25 total records/)).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: 'First page' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Next page' })).toBeEnabled();
      expect(screen.getByRole('button', { name: 'Last page' })).toBeEnabled();
    });

    it('navigates to next page when Next button clicked', async () => {
      const user = userEvent.setup();
      const allItems = generateItems(25);

      setupMswHandler((url) => {
        const page = Number(url.searchParams.get('page') ?? '0');
        const size = Number(url.searchParams.get('size') ?? '10');
        const slice = allItems.slice(page * size, (page + 1) * size);
        return createPageResponse(slice, page, size, 25);
      });

      renderDataTable();

      await waitFor(() => {
        expect(screen.getByText('Item 1')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: 'Next page' }));

      await waitFor(() => {
        expect(screen.getByText('Item 11')).toBeInTheDocument();
      });
      expect(screen.getByText(/Page 2 of 3/)).toBeInTheDocument();
    });

    it('navigates to last page and disables Next/Last buttons', async () => {
      const user = userEvent.setup();
      const allItems = generateItems(25);

      setupMswHandler((url) => {
        const page = Number(url.searchParams.get('page') ?? '0');
        const size = Number(url.searchParams.get('size') ?? '10');
        const slice = allItems.slice(page * size, (page + 1) * size);
        return createPageResponse(slice, page, size, 25);
      });

      renderDataTable();

      await waitFor(() => {
        expect(screen.getByText(/Page 1 of 3/)).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: 'Last page' }));

      await waitFor(() => {
        expect(screen.getByText(/Page 3 of 3/)).toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Last page' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'First page' })).toBeEnabled();
      expect(screen.getByRole('button', { name: 'Previous page' })).toBeEnabled();
    });

    it('changes page size and resets to first page', async () => {
      const user = userEvent.setup();
      const allItems = generateItems(50);

      setupMswHandler((url) => {
        const page = Number(url.searchParams.get('page') ?? '0');
        const size = Number(url.searchParams.get('size') ?? '10');
        const slice = allItems.slice(page * size, (page + 1) * size);
        return createPageResponse(slice, page, size, 50);
      });

      renderDataTable();

      await waitFor(() => {
        expect(screen.getByText(/Page 1 of 5/)).toBeInTheDocument();
      });

      // Change page size to 25
      await user.selectOptions(screen.getByRole('combobox', { name: 'Page size' }), '25');

      await waitFor(() => {
        expect(screen.getByText(/Page 1 of 2/)).toBeInTheDocument();
      });
    });
  });

  describe('Sorting toggle behavior', () => {
    it('cycles through ascending → descending → unsorted on header click', async () => {
      const user = userEvent.setup();

      setupMswHandler(() =>
        createPageResponse(generateItems(5), 0, 10, 5),
      );

      renderDataTable();

      await waitFor(() => {
        expect(screen.getByText('Item 1')).toBeInTheDocument();
      });

      const nameHeader = screen.getByRole('columnheader', { name: /Name/ });

      // Initially unsorted
      expect(nameHeader).toHaveAttribute('aria-sort', 'none');

      // Click 1: ascending
      await user.click(nameHeader);
      await waitFor(() => {
        expect(nameHeader).toHaveAttribute('aria-sort', 'ascending');
      });

      // Click 2: descending
      await user.click(nameHeader);
      await waitFor(() => {
        expect(nameHeader).toHaveAttribute('aria-sort', 'descending');
      });

      // Click 3: unsorted
      await user.click(nameHeader);
      await waitFor(() => {
        expect(nameHeader).toHaveAttribute('aria-sort', 'none');
      });
    });

    it('sends sort param to the API when sorting is active', async () => {
      const user = userEvent.setup();
      let capturedSort: string | null = null;

      server.use(
        http.get(MSW_URL, ({ request }) => {
          const url = new URL(request.url);
          capturedSort = url.searchParams.get('sort');
          return HttpResponse.json(
            createPageResponse(generateItems(5), 0, 10, 5),
          );
        }),
      );

      renderDataTable();

      await waitFor(() => {
        expect(screen.getByText('Item 1')).toBeInTheDocument();
      });

      const nameHeader = screen.getByRole('columnheader', { name: /Name/ });
      await user.click(nameHeader);

      await waitFor(() => {
        expect(capturedSort).toBe('name,asc');
      });
    });
  });

  describe('Filter debounce behavior', () => {
    it('debounces text filter input by 300ms before fetching', async () => {
      const user = userEvent.setup();
      vi.useFakeTimers({ shouldAdvanceTime: true });

      let requestCount = 0;
      let lastFilterValue: string | null = null;

      server.use(
        http.get(MSW_URL, ({ request }) => {
          requestCount++;
          const url = new URL(request.url);
          lastFilterValue = url.searchParams.get('city');
          return HttpResponse.json(
            createPageResponse(generateItems(3), 0, 10, 3),
          );
        }),
      );

      // Use columns where only city has filtering enabled
      const filterableColumns: ColumnDef<TestItem, unknown>[] = [
        { accessorKey: 'id', header: 'ID', enableColumnFilter: false },
        { accessorKey: 'name', header: 'Name', enableColumnFilter: false },
        { accessorKey: 'status', header: 'Status', enableColumnFilter: false },
        { accessorKey: 'city', header: 'City', enableColumnFilter: true },
      ];

      renderWithProviders(
        <DataTable<TestItem>
          columns={filterableColumns}
          queryKey={['test-debounce']}
          endpoint={COMPONENT_ENDPOINT}
          defaultPageSize={10}
        />,
      );

      // Wait for initial load
      await waitFor(() => {
        expect(requestCount).toBeGreaterThanOrEqual(1);
      });
      const initialCount = requestCount;

      // Type into the filter quickly
      const filterInput = screen.getByRole('textbox', { name: /Filter city/ });
      await user.type(filterInput, 'Mum');

      // Advance time less than debounce — should not trigger another fetch
      await vi.advanceTimersByTimeAsync(200);
      expect(requestCount).toBe(initialCount);

      // Advance past debounce threshold
      await vi.advanceTimersByTimeAsync(200);

      await waitFor(() => {
        expect(lastFilterValue).toBe('Mum');
      });

      vi.useRealTimers();
    });

    it('renders select filter for columns with filterType select', async () => {
      setupMswHandler(() =>
        createPageResponse(generateItems(5), 0, 10, 5),
      );

      renderDataTable();

      await waitFor(() => {
        expect(screen.getByText('Item 1')).toBeInTheDocument();
      });

      // The status column has filterType: 'select'
      const selectFilter = screen.getByRole('combobox', { name: /Filter status/ });
      expect(selectFilter).toBeInTheDocument();
      expect(within(selectFilter).getByText('All')).toBeInTheDocument();
      expect(within(selectFilter).getByText('Active')).toBeInTheDocument();
      expect(within(selectFilter).getByText('Draft')).toBeInTheDocument();
    });
  });

  describe('Empty state and error state rendering', () => {
    it('renders empty state message when no records returned', async () => {
      setupMswHandler(() =>
        createPageResponse([], 0, 10, 0),
      );

      renderDataTable({ emptyMessage: 'No items available.' });

      await waitFor(() => {
        expect(screen.getByText('No items available.')).toBeInTheDocument();
      });
    });

    it('renders default empty message when emptyMessage prop not provided', async () => {
      setupMswHandler(() =>
        createPageResponse([], 0, 10, 0),
      );

      renderDataTable();

      await waitFor(() => {
        expect(screen.getByText('No records found.')).toBeInTheDocument();
      });
    });

    it('renders error state with message and retry button on failed query', async () => {
      server.use(
        http.get(MSW_URL, () => {
          return HttpResponse.json(
            { error: 'Internal Server Error', message: 'Something went wrong' },
            { status: 500 },
          );
        }),
      );

      renderDataTable();

      // The error interceptor normalizes the error — wait for error state to appear
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Retry/ })).toBeInTheDocument();
      });
      // Error message is displayed (extracted from the normalized error)
      expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    });

    it('retries fetch when Retry button is clicked after error', async () => {
      let callCount = 0;

      server.use(
        http.get(MSW_URL, () => {
          callCount++;
          if (callCount === 1) {
            return HttpResponse.json(
              { message: 'Server error' },
              { status: 500 },
            );
          }
          return HttpResponse.json(
            createPageResponse(generateItems(3), 0, 10, 3),
          );
        }),
      );

      const user = userEvent.setup();
      renderDataTable();

      // Wait for error state
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Retry/ })).toBeInTheDocument();
      });

      // Click retry
      await user.click(screen.getByRole('button', { name: /Retry/ }));

      // Should load data successfully
      await waitFor(() => {
        expect(screen.getByText('Item 1')).toBeInTheDocument();
      });
    });
  });

  describe('Column visibility toggling', () => {
    it('shows Columns button when enableColumnVisibility is true', async () => {
      setupMswHandler(() =>
        createPageResponse(generateItems(3), 0, 10, 3),
      );

      renderDataTable({ enableColumnVisibility: true });

      await waitFor(() => {
        expect(screen.getByText('Item 1')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /Columns/ })).toBeInTheDocument();
    });

    it('hides Columns button when enableColumnVisibility is false', async () => {
      // Use columns without any filters to avoid toolbar rendering from filters
      const noFilterColumns: ColumnDef<TestItem, unknown>[] = [
        { accessorKey: 'id', header: 'ID', enableColumnFilter: false },
        { accessorKey: 'name', header: 'Name', enableColumnFilter: false },
      ];

      setupMswHandler(() =>
        createPageResponse(generateItems(3), 0, 10, 3),
      );

      renderWithProviders(
        <DataTable<TestItem>
          columns={noFilterColumns}
          queryKey={['test-no-vis']}
          endpoint={COMPONENT_ENDPOINT}
          enableColumnVisibility={false}
        />,
      );

      await waitFor(() => {
        expect(screen.getByText('Item 1')).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: /Columns/ })).not.toBeInTheDocument();
    });

    it('toggles column visibility when checkbox is unchecked', async () => {
      const user = userEvent.setup();

      setupMswHandler(() =>
        createPageResponse(generateItems(3), 0, 10, 3),
      );

      renderDataTable();

      await waitFor(() => {
        expect(screen.getByText('Item 1')).toBeInTheDocument();
      });

      // Open visibility dropdown
      await user.click(screen.getByRole('button', { name: /Columns/ }));

      // City column should be visible
      expect(screen.getByRole('columnheader', { name: /City/ })).toBeInTheDocument();

      // Uncheck City column
      const cityCheckbox = screen.getByRole('menuitemcheckbox', { name: /City/ });
      const checkbox = within(cityCheckbox).getByRole('checkbox');
      await user.click(checkbox);

      // City column should be hidden
      expect(screen.queryByRole('columnheader', { name: /City/ })).not.toBeInTheDocument();
    });

    it('prevents hiding the last visible column', async () => {
      const user = userEvent.setup();

      // Use only two columns for easy testing
      const twoColumns: ColumnDef<TestItem, unknown>[] = [
        { accessorKey: 'id', header: 'ID' },
        { accessorKey: 'name', header: 'Name' },
      ];

      setupMswHandler(() =>
        createPageResponse(generateItems(2), 0, 10, 2),
      );

      renderWithProviders(
        <DataTable<TestItem>
          columns={twoColumns}
          queryKey={['test-last-col']}
          endpoint={COMPONENT_ENDPOINT}
          enableColumnVisibility={true}
        />,
      );

      await waitFor(() => {
        expect(screen.getByText('Item 1')).toBeInTheDocument();
      });

      // Open visibility dropdown
      await user.click(screen.getByRole('button', { name: /Columns/ }));

      // Hide the first column (ID)
      const idCheckbox = within(
        screen.getByRole('menuitemcheckbox', { name: /ID/ }),
      ).getByRole('checkbox');
      await user.click(idCheckbox);

      // ID column should be hidden now
      expect(screen.queryByRole('columnheader', { name: /^ID$/ })).not.toBeInTheDocument();

      // Name column should still be visible — its checkbox should be disabled
      const nameCheckbox = within(
        screen.getByRole('menuitemcheckbox', { name: /Name/ }),
      ).getByRole('checkbox');
      expect(nameCheckbox).toBeDisabled();
    });
  });
});
