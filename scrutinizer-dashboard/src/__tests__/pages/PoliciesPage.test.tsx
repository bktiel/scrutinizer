import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import PoliciesPage from '../../pages/PoliciesPage'

function renderWithRouter() {
  return render(
    <MemoryRouter>
      <PoliciesPage />
    </MemoryRouter>
  )
}

describe('PoliciesPage', () => {
  it('renders the policies heading', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/policies/i)).toBeInTheDocument()
    })
  })

  it('loads and displays policies', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText('npm-baseline')).toBeInTheDocument()
    })
  })

  it('displays Create Policy button', async () => {
    renderWithRouter()
    await waitFor(() => {
      // Implemented as a Button with onClick={navigate}, not an <a> link.
      expect(screen.getByRole('button', { name: /create policy/i })).toBeInTheDocument()
    })
  })
})
