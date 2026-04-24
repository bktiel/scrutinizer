import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import RunDetailPage from '../../pages/RunDetailPage'

function renderWithRouter(runId = 'run-1') {
  return render(
    <MemoryRouter initialEntries={[`/runs/${runId}`]}>
      <Routes>
        <Route path="/runs/:id" element={<RunDetailPage />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('RunDetailPage', () => {
  it('loads and displays run details', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText('test-app')).toBeInTheDocument()
    })
  })

  it('displays the policy name', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/npm-baseline/)).toBeInTheDocument()
    })
  })

  it('displays the overall decision', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText('PASS')).toBeInTheDocument()
    })
  })

  it('displays component results', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText('express')).toBeInTheDocument()
    })
  })

  it('displays posture score', async () => {
    renderWithRouter()
    await waitFor(() => {
      expect(screen.getByText(/85\.5/)).toBeInTheDocument()
    })
  })
})
