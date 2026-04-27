import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ExceptionsPage from '../../pages/ExceptionsPage'

function renderWithRouter() {
  return render(
    <MemoryRouter>
      <ExceptionsPage />
    </MemoryRouter>
  )
}

describe('ExceptionsPage', () => {
  it('renders exceptions heading', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/exceptions/i)).toBeInTheDocument()
    })
  })

  it('loads and displays exceptions', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/scorecard-min/)).toBeInTheDocument()
    })
  })

  it('displays exception status', async () => {
    renderWithRouter()
    await waitFor(() => {
      // 'ACTIVE' appears in the filter chip and on each row; assert at least one is shown.
      expect(screen.getAllByText('ACTIVE').length).toBeGreaterThan(0)
    })
  })

  it('has a create exception button', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /create|new|add/i })).toBeInTheDocument()
    })
  })
})
