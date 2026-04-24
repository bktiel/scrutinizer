import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import OverviewDashboardPage from '../../pages/OverviewDashboardPage'

function renderWithRouter() {
  return render(
    <MemoryRouter>
      <OverviewDashboardPage />
    </MemoryRouter>
  )
}

describe('OverviewDashboardPage', () => {
  it('renders the overview page', async () => {
    renderWithRouter()
    await waitFor(() => {
      // Should show some dashboard content
      expect(screen.getByText(/dashboard|overview|scrutinizer/i)).toBeInTheDocument()
    })
  })

  it('loads and displays project data', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/test-project/)).toBeInTheDocument()
    })
  })
})
