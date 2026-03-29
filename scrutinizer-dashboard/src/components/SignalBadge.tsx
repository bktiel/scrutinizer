import { Chip } from '@mui/material'

interface SignalBadgeProps {
  value: string | number
  type: 'decision' | 'score' | 'boolean'
}

export default function SignalBadge({ value, type }: SignalBadgeProps) {
  if (type === 'decision') {
    const colorMap: Record<string, 'success' | 'warning' | 'error' | 'info' | 'default'> = {
      PASS: 'success',
      WARN: 'warning',
      FAIL: 'error',
      INFO: 'info',
      SKIP: 'default',
    }
    return <Chip label={value} color={colorMap[String(value)] ?? 'default'} size="small" />
  }

  if (type === 'score') {
    const num = typeof value === 'number' ? value : parseFloat(String(value))
    const color = num > 7 ? 'success' : num > 4 ? 'warning' : 'error'
    return <Chip label={num.toFixed(1)} color={color} size="small" variant="outlined" />
  }

  if (type === 'boolean') {
    const isTrue = value === true || value === 'true'
    return <Chip label={isTrue ? '\u2713' : '\u2717'} color={isTrue ? 'success' : 'error'} size="small" />
  }

  return <Chip label={String(value)} size="small" />
}
