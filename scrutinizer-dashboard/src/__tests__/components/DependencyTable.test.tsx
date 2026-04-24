import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import DependencyTable from '../../components/DependencyTable'
import type { ComponentResult } from '../../api/scrutinizerApi'

const sampleComponents: ComponentResult[] = [
  {
    id: 'cr-1',
    componentRef: 'express@4.18.2',
    componentName: 'express',
    componentVersion: '4.18.2',
    purl: 'pkg:npm/express@4.18.2',
    isDirect: true,
    decision: 'PASS',
    findings: [],
  },
  {
    id: 'cr-2',
    componentRef: 'lodash@4.17.21',
    componentName: 'lodash',
    componentVersion: '4.17.21',
    purl: 'pkg:npm/lodash@4.17.21',
    isDirect: false,
    decision: 'FAIL',
    findings: [],
  },
]

describe('DependencyTable', () => {
  it('renders component names', () => {
    render(<DependencyTable components={sampleComponents} />)
    expect(screen.getByText('express')).toBeInTheDocument()
    expect(screen.getByText('lodash')).toBeInTheDocument()
  })

  it('renders component versions', () => {
    render(<DependencyTable components={sampleComponents} />)
    expect(screen.getByText('4.18.2')).toBeInTheDocument()
    expect(screen.getByText('4.17.21')).toBeInTheDocument()
  })

  it('renders decisions', () => {
    render(<DependencyTable components={sampleComponents} />)
    expect(screen.getByText('PASS')).toBeInTheDocument()
    expect(screen.getByText('FAIL')).toBeInTheDocument()
  })

  it('handles empty components list', () => {
    render(<DependencyTable components={[]} />)
    // Should render table structure but no data rows
    expect(screen.queryByText('express')).not.toBeInTheDocument()
  })
})
