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
      expect(screen.getByText('ACTIVE')).toBeInTheDocument()
    })
  })

  it('has a create exception button', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /create|new|add/i })).toBeInTheDocument()
    })
  })
})
