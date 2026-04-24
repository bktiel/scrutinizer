import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ProjectsPage from '../../pages/ProjectsPage'

function renderWithRouter() {
  return render(
    <MemoryRouter>
      <ProjectsPage />
    </MemoryRouter>
  )
}

describe('ProjectsPage', () => {
  it('renders page heading', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/projects/i)).toBeInTheDocument()
    })
  })

  it('loads and displays project names', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText('test-project')).toBeInTheDocument()
    })
  })

  it('displays project statistics', async () => {
    renderWithRouter()
    await waitFor(() => {
      // Stats should show scores or run counts
      expect(screen.getByText(/85\.5/)).toBeInTheDocument()
    })
  })

  it('has a register project button', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /register|new|add/i })).toBeInTheDocument()
    })
  })
})
