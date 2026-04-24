import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import PolicyYamlPreview from '../../components/PolicyYamlPreview'

describe('PolicyYamlPreview', () => {
  const sampleYaml = 'apiVersion: scrutinizer/v1\nmetadata:\n  name: test'

  it('renders nothing when not open', () => {
    render(<PolicyYamlPreview open={false} yaml={sampleYaml} onClose={() => {}} />)
    expect(screen.queryByText('Policy YAML Preview')).not.toBeInTheDocument()
  })

  it('renders dialog when open', () => {
    render(<PolicyYamlPreview open={true} yaml={sampleYaml} onClose={() => {}} />)
    expect(screen.getByText('Policy YAML Preview')).toBeInTheDocument()
  })

  it('displays yaml content', () => {
    render(<PolicyYamlPreview open={true} yaml={sampleYaml} onClose={() => {}} />)
    expect(screen.getByText(/apiVersion: scrutinizer\/v1/)).toBeInTheDocument()
  })

  it('calls onClose when Close button is clicked', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(<PolicyYamlPreview open={true} yaml={sampleYaml} onClose={onClose} />)
    await user.click(screen.getByRole('button', { name: /close/i }))
    expect(onClose).toHaveBeenCalled()
  })

  it('has a copy button', () => {
    render(<PolicyYamlPreview open={true} yaml={sampleYaml} onClose={() => {}} />)
    expect(screen.getByRole('button', { name: /copy/i })).toBeInTheDocument()
  })
})
