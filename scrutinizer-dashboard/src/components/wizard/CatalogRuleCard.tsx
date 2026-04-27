import { Card, CardContent, Stack, TextField, Select, MenuItem, IconButton, Box, Typography, Chip } from '@mui/material'
import DeleteIcon from '@mui/icons-material/Delete'
import type { Rule } from '../RuleCard'
import { ECOSYSTEM_OPTIONS } from '../RuleCard'
import { CatalogEntry, extractParamsFromRule } from '../../data/ruleCatalog'

const SEVERITY_OPTIONS: { value: Rule['severity']; label: string; color: 'error' | 'warning' | 'info' | 'default' }[] = [
  { value: 'FAIL', label: 'Block', color: 'error' },
  { value: 'WARN', label: 'Warn', color: 'warning' },
  { value: 'INFO', label: 'Info', color: 'info' },
  { value: 'SKIP', label: 'Disabled', color: 'default' },
]

const TARGET_LABELS: Record<string, string> = {
  ALL: 'all',
  DIRECT: 'direct only',
  TRANSITIVE: 'transitive only',
}

interface Props {
  rule: Rule
  catalogEntry: CatalogEntry
  onChange: (rule: Rule) => void
  onDelete: () => void
}

export default function CatalogRuleCard({ rule, catalogEntry, onChange, onDelete }: Props) {
  const params = extractParamsFromRule(rule, catalogEntry)

  const handleParamChange = (key: string, value: string) => {
    const newParams = { ...params, [key]: value }
    const newRule = catalogEntry.toRule(newParams)
    // Preserve the current severity and target from user's choice
    onChange({ ...newRule, severity: rule.severity, target: rule.target })
  }

  const handleSeverityChange = (severity: Rule['severity']) => {
    onChange({ ...rule, severity })
  }

  const handleTargetChange = (target: Rule['target']) => {
    onChange({ ...rule, target })
  }

  const handleEcosystemChange = (value: string) => {
    onChange({ ...rule, ecosystem: value || undefined })
  }

  // Build the human-readable sentence with inline inputs
  const renderSentence = () => {
    const parts = catalogEntry.sentence.split(/(\{[^}]+\})/)
    return parts.map((part, i) => {
      const match = part.match(/^\{([^}]+)\}$/)
      if (!match) {
        return (
          <Typography key={i} component="span" variant="body1" sx={{ color: 'text.primary' }}>
            {part}
          </Typography>
        )
      }
      const paramKey = match[1]
      const param = catalogEntry.parameters.find((p) => p.key === paramKey)
      if (!param) return null

      return (
        <TextField
          key={i}
          size="small"
          variant="outlined"
          value={params[paramKey] || ''}
          onChange={(e) => handleParamChange(paramKey, e.target.value)}
          placeholder={param.placeholder || param.label}
          type={param.type === 'number' ? 'number' : 'text'}
          sx={{
            mx: 0.5,
            display: 'inline-flex',
            width: param.type === 'number' ? 80 : param.type === 'text-list' ? 240 : 160,
            verticalAlign: 'middle',
            '& .MuiInputBase-input': { py: 0.5, px: 1, fontSize: '0.9rem' },
          }}
          inputProps={param.type === 'number' ? { min: 0, max: 10, step: 0.5 } : undefined}
        />
      )
    })
  }

  return (
    <Card
      variant="outlined"
      sx={{
        borderColor: 'rgba(0, 184, 212, 0.15)',
        bgcolor: 'rgba(0, 184, 212, 0.02)',
      }}
    >
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Stack spacing={1.5}>
          {/* Human-readable sentence with inline params */}
          <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 0.3, lineHeight: 2.2 }}>
            {renderSentence()}
            <Typography component="span" variant="body1" sx={{ color: 'text.secondary', ml: 0.5 }}>
              for
            </Typography>
            <Select
              size="small"
              value={rule.target}
              onChange={(e) => handleTargetChange(e.target.value as Rule['target'])}
              sx={{ mx: 0.5, '& .MuiSelect-select': { py: 0.5, px: 1, fontSize: '0.9rem' } }}
            >
              {Object.entries(TARGET_LABELS).map(([value, label]) => (
                <MenuItem key={value} value={value}>
                  {label}
                </MenuItem>
              ))}
            </Select>
            <Typography component="span" variant="body1" sx={{ color: 'text.secondary' }}>
              dependencies
            </Typography>
            <Select
              size="small"
              value={rule.ecosystem || ''}
              onChange={(e) => handleEcosystemChange(e.target.value)}
              displayEmpty
              sx={{ mx: 0.5, '& .MuiSelect-select': { py: 0.5, px: 1, fontSize: '0.85rem' } }}
            >
              {ECOSYSTEM_OPTIONS.map((opt) => (
                <MenuItem key={opt.value} value={opt.value} sx={{ fontSize: '0.85rem' }}>
                  {opt.value ? `(${opt.label} only)` : '(all ecosystems)'}
                </MenuItem>
              ))}
            </Select>
          </Box>

          {/* Severity chips + delete */}
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Stack direction="row" spacing={0.5}>
              {SEVERITY_OPTIONS.map((opt) => (
                <Chip
                  key={opt.value}
                  label={opt.label}
                  size="small"
                  color={rule.severity === opt.value ? opt.color : 'default'}
                  variant={rule.severity === opt.value ? 'filled' : 'outlined'}
                  onClick={() => handleSeverityChange(opt.value)}
                  sx={{
                    cursor: 'pointer',
                    fontWeight: rule.severity === opt.value ? 700 : 400,
                    opacity: rule.severity === opt.value ? 1 : 0.6,
                  }}
                />
              ))}
            </Stack>
            <IconButton size="small" onClick={onDelete} color="error" sx={{ opacity: 0.6, '&:hover': { opacity: 1 } }}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  )
}
