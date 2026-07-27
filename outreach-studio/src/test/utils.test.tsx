import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from './utils'

describe('renderWithProviders', () => {
  it('renders a component within providers', () => {
    renderWithProviders(<div data-testid="test-child">Hello</div>)
    expect(screen.getByTestId('test-child')).toHaveTextContent('Hello')
  })

  it('returns a queryClient instance', () => {
    const { queryClient } = renderWithProviders(<div>Test</div>)
    expect(queryClient).toBeDefined()
    expect(queryClient.getDefaultOptions().queries?.retry).toBe(false)
  })
})
