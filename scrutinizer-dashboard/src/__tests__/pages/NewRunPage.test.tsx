import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import NewRunPage from '../../pages/NewRunPage'

function renderWithRouter() {
  return render(
    <MemoryRouter>
      <NewRunPage />
    </MemoryRouter>
  )
}

describe('NewRunPage', () => {
  it('renders the new run form', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/new.*run|evaluate|submit/i)).toBeInTheDocument()
    })
  })

  it('loads policies for selection', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/policy/i)).toBeInTheDocument()
    })
  })

  it('has an SBOM upload area', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/sbom|upload|file/i)).toBeInTheDocument()
    })
  })

  it('has a submit button', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /run|evaluate|submit/i })).toBeInTheDocument()
    })
  })
})
