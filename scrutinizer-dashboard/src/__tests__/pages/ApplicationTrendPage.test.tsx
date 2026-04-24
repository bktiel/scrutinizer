import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import ApplicationTrendPage from '../../pages/ApplicationTrendPage'

function renderWithRouter(appName = 'test-app') {
  return render(
    <MemoryRouter initialEntries={[`/trends/${appName}`]}>
      <Routes>
        <Route path="/trends/:applicationName" element={<ApplicationTrendPage />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('ApplicationTrendPage', () => {
  it('renders the page with application name', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/test-app/)).toBeInTheDocument()
    })
  })

  it('loads trend data', async () => {
    renderWithRouter()
    await waitFor(() => {
      // Should show some trend-related content
      expect(screen.getByText(/trend|score|posture/i)).toBeInTheDocument()
    })
  })
})
