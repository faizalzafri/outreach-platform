import { render } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { RenderOptions } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'

/**
 * Placeholder AuthProvider for tests.
 * Will be replaced with the real AuthProvider once the Auth module is implemented.
 */
function AuthProvider({ children }: { children: ReactNode }) {
  return <>{children}</>
}

/**
 * Placeholder Router wrapper for tests.
 * Will be replaced with the real TanStack Router wrapper once routing is implemented.
 */
function RouterWrapper({ children }: { children: ReactNode }) {
  return <div>{children}</div>
}

/**
 * Creates a fresh QueryClient configured for testing:
 * - No retries (fail fast in tests)
 * - No garbage collection delay
 */
function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  })
}

interface RenderWithProvidersOptions extends Omit<RenderOptions, 'wrapper'> {
  queryClient?: QueryClient
}

/**
 * Renders a component wrapped in all app-level providers:
 * - QueryClientProvider (TanStack Query)
 * - AuthProvider (authentication context)
 * - RouterWrapper (routing context)
 *
 * Use this helper for integration-style tests that need provider context.
 */
export function renderWithProviders(
  ui: ReactElement,
  options: RenderWithProvidersOptions = {},
) {
  const { queryClient = createTestQueryClient(), ...renderOptions } = options

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <RouterWrapper>{children}</RouterWrapper>
        </AuthProvider>
      </QueryClientProvider>
    )
  }

  return {
    ...render(ui, { wrapper: Wrapper, ...renderOptions }),
    queryClient,
  }
}

export { createTestQueryClient }
