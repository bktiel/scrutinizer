import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import CancelIcon from '@mui/icons-material/Cancel'
import type { PolicyForm } from '../../utils/policyYaml'
import type { Rule } from '../RuleCard'
import { SAMPLE_PACKAGES, SamplePackage } from '../../data/samplePackages'

type Verdict = 'PASS' | 'WARN' | 'FAIL' | 'SKIP'

function getFieldValue(pkg: SamplePackage, field: string): string | number | boolean | undefined {
  if (field === 'scorecard.score') return pkg['scorecard.score']
  if (field === 'provenance.present') return pkg['provenance.present']
  const key = field as keyof SamplePackage
  return pkg[key]
}

function evaluateRule(rule: Rule, pkg: SamplePackage): boolean {
  const raw = getFieldValue(pkg, rule.field)

  if (rule.operator === 'EXISTS') {
    return raw !== undefined && raw !== '' && raw !== null
  }

  if (raw === undefined || raw === null) return false

  const fieldStr = String(raw)
  const fieldNum = Number(raw)
  const valueNum = Number(rule.value)

  switch (rule.operator) {
    case 'EQ':
      return fieldStr === rule.value || (typeof raw === 'boolean' && String(raw) === rule.value)
    case 'NEQ':
      return fieldStr !== rule.value
    case 'GT':
      return !isNaN(fieldNum) && !isNaN(valueNum) && fieldNum > valueNum
    case 'GTE':
      return !isNaN(fieldNum) && !isNaN(valueNum) && fieldNum >= valueNum
    case 'LT':
      return !isNaN(fieldNum) && !isNaN(valueNum) && fieldNum < valueNum
    case 'LTE':
      return !isNaN(fieldNum) && !isNaN(valueNum) && fieldNum <= valueNum
    case 'IN': {
      const list = rule.value.split(',').map((s) => s.trim())
      return list.includes(fieldStr)
    }
    case 'NOT_IN': {
      const list = rule.value.split(',').map((s) => s.trim())
      return !list.includes(fieldStr)
    }
    default:
      return true
  }
}

function simulateVerdict(rules: Rule[], pkg: SamplePackage): Verdict {
  let worst: Verdict = 'PASS'

  for (const rule of rules) {
    if (rule.severity === 'SKIP') continue

    const passes = evaluateRule(rule, pkg)

    // For ban rules (name EQ/IN with FAIL), the rule FAILS if the package matches
    const isBanRule = rule.field === 'name' && (rule.operator === 'EQ' || rule.operator === 'IN')
    const ruleOk = isBanRule ? !passes : passes

    if (!ruleOk) {
      if (rule.severity === 'FAIL' && worst !== 'FAIL') worst = 'FAIL'
      if (rule.severity === 'WARN' && worst === 'PASS') worst = 'WARN'
      // INFO doesn't change verdict
    }
  }

  return worst
}

interface Props {
  form: PolicyForm
}

export default function ImpactPreview({ form }: Props) {
  if (form.rules.length === 0) {
    return (
      <Box sx={{ border: '1px dashed rgba(139,148,158,0.3)', borderRadius: 2, p: 3, textAlign: 'center' }}>
        <Typography color="text.secondary" variant="body2">
          Add rules to see how they would evaluate against sample packages.
        </Typography>
      </Box>
    )
  }

  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
        Policy Impact Preview
      </Typography>
      <Typography variant="caption" color="text.secondary" sx={{ mb: 2, display: 'block' }}>
        Simulated results against well-known packages. Shows how your rules would evaluate each one.
      </Typography>

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 700 }}>Package</TableCell>
              <TableCell sx={{ fontWeight: 700 }} align="center">Scorecard</TableCell>
              <TableCell sx={{ fontWeight: 700 }} align="center">Provenance</TableCell>
              <TableCell sx={{ fontWeight: 700 }} align="center">Verdict</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {SAMPLE_PACKAGES.map((pkg) => {
              const verdict = simulateVerdict(form.rules, pkg)
              return (
                <TableRow key={pkg.name}>
                  <TableCell>
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {pkg.name}@{pkg.version}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {pkg.label}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Typography
                      variant="body2"
                      sx={{
                        fontWeight: 600,
                        color: pkg['scorecard.score'] >= 7 ? '#00E676' : pkg['scorecard.score'] >= 4 ? '#FFAB00' : '#FF5252',
                      }}
                    >
                      {pkg['scorecard.score'].toFixed(1)}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    {pkg['provenance.present'] ? (
                      <CheckCircleIcon sx={{ fontSize: 18, color: '#00E676' }} />
                    ) : (
                      <CancelIcon sx={{ fontSize: 18, color: '#FF5252', opacity: 0.6 }} />
                    )}
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={verdict}
                      size="small"
                      color={verdict === 'PASS' ? 'success' : verdict === 'WARN' ? 'warning' : 'error'}
                      sx={{ fontWeight: 700, fontSize: '0.7rem' }}
                    />
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )
}
