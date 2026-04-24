import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import FindingsPage from '../../pages/FindingsPage'

function renderWithRouter(runId = 'run-1') {
  return render(
    <MemoryRouter initialEntries={[`/runs/${runId}/findings`]}>
      <Routes>
        <Route path="/runs/:id/findings" element={<FindingsPage />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('FindingsPage', () => {
  it('loads and displays findings', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/scorecard-min/)).toBeInTheDocument()
    })
  })

  it('displays finding decisions', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText('FAIL')).toBeInTheDocument()
    })
  })

  it('displays component names', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText('lodash')).toBeInTheDocument()
    })
  })
})
