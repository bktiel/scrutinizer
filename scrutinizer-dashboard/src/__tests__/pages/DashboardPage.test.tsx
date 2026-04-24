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
      expect(screen.getByText(/posture runs/i)).toBeInTheDocument()
    })
  })

  it('loads and displays run summaries', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText('test-app')).toBeInTheDocument()
    })
  })

  it('displays policy names', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText('npm-baseline')).toBeInTheDocument()
    })
  })
})
