import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import SignalBadge from '../../components/SignalBadge'

describe('SignalBadge', () => {
  describe('decision type', () => {
    it('renders PASS badge with success color', () => {
      render(<SignalBadge value="PASS" type="decision" />)
      expect(screen.getByText('PASS')).toBeInTheDocument()
    })

    it('renders FAIL badge', () => {
      render(<SignalBadge value="FAIL" type="decision" />)
      expect(screen.getByText('FAIL')).toBeInTheDocument()
    })

    it('renders WARN badge', () => {
      render(<SignalBadge value="WARN" type="decision" />)
      expect(screen.getByText('WARN')).toBeInTheDocument()
    })

    it('renders INFO badge', () => {
      render(<SignalBadge value="INFO" type="decision" />)
      expect(screen.getByText('INFO')).toBeInTheDocument()
    })

    it('renders SKIP badge', () => {
      render(<SignalBadge value="SKIP" type="decision" />)
      expect(screen.getByText('SKIP')).toBeInTheDocument()
    })

    it('renders unknown decision with default color', () => {
      render(<SignalBadge value="UNKNOWN" type="decision" />)
      expect(screen.getByText('UNKNOWN')).toBeInTheDocument()
    })
  })

  describe('score type', () => {
    it('renders high score with one decimal', () => {
      render(<SignalBadge value={8.5} type="score" />)
      expect(screen.getByText('8.5')).toBeInTheDocument()
    })

    it('renders medium score', () => {
      render(<SignalBadge value={5.0} type="score" />)
      expect(screen.getByText('5.0')).toBeInTheDocument()
    })

    it('renders low score', () => {
      render(<SignalBadge value={2.3} type="score" />)
      expect(screen.getByText('2.3')).toBeInTheDocument()
    })

    it('handles string score values', () => {
      render(<SignalBadge value="7.5" type="score" />)
      expect(screen.getByText('7.5')).toBeInTheDocument()
    })
  })

  describe('boolean type', () => {
    it('renders checkmark for true', () => {
      render(<SignalBadge value="true" type="boolean" />)
      expect(screen.getByText('✓')).toBeInTheDocument()
    })

    it('renders X for false', () => {
      render(<SignalBadge value="false" type="boolean" />)
      expect(screen.getByText('✗')).toBeInTheDocument()
    })
  })
})
