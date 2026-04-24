import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import App from '../../App'

describe('App', () => {
  it('renders the app bar with Scrutinizer title', () => {
    render(<App />)
    expect(screen.getByText('Scrutinizer')).toBeInTheDocument()
  })

  it('renders navigation links', () => {
    render(<App />)
    expect(screen.getByRole('link', { name: /dashboard/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /projects/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /policies/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /exceptions/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /new run/i })).toBeInTheDocument()
  })

  it('renders Dashboard link pointing to /', () => {
    render(<App />)
    const dashboardLink = screen.getByRole('link', { name: /dashboard/i })
    expect(dashboardLink).toHaveAttribute('href', '/')
  })

  it('renders Projects link pointing to /projects', () => {
    render(<App />)
    const projectsLink = screen.getByRole('link', { name: /projects/i })
    expect(projectsLink).toHaveAttribute('href', '/projects')
  })

  it('renders Policies link pointing to /policies', () => {
    render(<App />)
    const policiesLink = screen.getByRole('link', { name: /policies/i })
    expect(policiesLink).toHaveAttribute('href', '/policies')
  })

  it('renders Exceptions link pointing to /exceptions', () => {
    render(<App />)
    const exceptionsLink = screen.getByRole('link', { name: /exceptions/i })
    expect(exceptionsLink).toHaveAttribute('href', '/exceptions')
  })

  it('renders New Run link pointing to /new-run', () => {
    render(<App />)
    const newRunLink = screen.getByRole('link', { name: /new run/i })
    expect(newRunLink).toHaveAttribute('href', '/new-run')
  })
})
