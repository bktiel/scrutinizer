import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import DashboardPage from '../../pages/DashboardPage'

function renderWithRouter(path = '/dashboard/runs') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <DashboardPage />
    </MemoryRouter>
  )
}

describe('DashboardPage', () => {
  it('renders the page heading', async () => {
    renderWithRouter()
    await waitFor(() => {
      // Heading is "Posture Evaluation Runs"
      expect(screen.getByRole('heading', { name: /posture/i })).toBeInTheDocument()
    })
  })

  it('loads and displays run summaries', async () => {
    renderWithRouter()
    await waitFor(() => {
      // Three fixture runs all share applicationName='test-app'; assert at least one renders.
      expect(screen.getAllByText('test-app').length).toBeGreaterThan(0)
    })
  })

  it('displays policy names', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getAllByText('npm-baseline').length).toBeGreaterThan(0)
    })
  })
})
