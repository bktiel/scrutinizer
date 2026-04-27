import {
  Card,
  CardContent,
  Stack,
  TextField,
  Select,
  MenuItem,
  IconButton,
  Box,
  Typography,
  Tooltip,
  Chip,
} from '@mui/material'
import DeleteIcon from '@mui/icons-material/Delete'
import InfoIcon from '@mui/icons-material/Info'

export interface Rule {
  id: string
  description: string
  field: string
  operator: 'EQ' | 'NEQ' | 'GT' | 'GTE' | 'LT' | 'LTE' | 'IN' | 'NOT_IN' | 'EXISTS'
  value: string
  severity: 'FAIL' | 'WARN' | 'INFO' | 'SKIP'
  target: 'ALL' | 'DIRECT' | 'TRANSITIVE'
  ecosystem?: string
}

export const ECOSYSTEM_OPTIONS = [
  { value: '', label: 'All Ecosystems' },
  { value: 'npm', label: 'npm' },
  { value: 'maven', label: 'Maven' },
  { value: 'pypi', label: 'PyPI' },
  { value: 'golang', label: 'Go' },
]

interface RuleCardProps {
  rule: Rule
  onChange: (rule: Rule) => void
  onDelete: () => void
}

const AVAILABLE_FIELDS = [
  'name',
  'version',
  'type',
  'group',
  'purl',
  'scope',
  'scorecard.score',
  'provenance.present',
]

const FIELD_DESCRIPTIONS: Record<string, string> = {
  name: "The component's name as declared in the SBOM (e.g., 'spring-core', 'lodash')",
  version: "The component's version string (e.g., '5.3.20', '4.17.21')",
  type: 'Component classification: library, framework, or application',
  group: "Maven group ID or npm scope (e.g., 'org.springframework', '@angular')",
  purl: "Package URL — a standardized identifier for software packages (e.g., 'pkg:maven/org.springframework/spring-core@5.3.20')",
  scope: 'Dependency scope: compile, test, runtime, provided, etc.',
  'scorecard.score':
    'OpenSSF Scorecard security health score (0-10). Higher is better. Measures project maintenance, vulnerability response, and security practices.',
  'provenance.present':
    'Whether the package has SLSA provenance attestation — a cryptographic proof of how and where the software was built.',
}

const OPERATOR_DESCRIPTIONS: Record<string, string> = {
  EQ: 'Equals — exact match',
  NEQ: 'Not equals — fails if the value matches',
  GT: 'Greater than — numeric comparison',
  GTE: 'Greater than or equal — numeric comparison',
  LT: 'Less than — numeric comparison',
  LTE: 'Less than or equal — numeric comparison',
  IN: 'In list — value must be one of comma-separated options',
  NOT_IN: 'Not in list — value must NOT be any of comma-separated options',
  EXISTS: 'Exists — checks if the field is present (no value needed)',
}

const SEVERITY_DESCRIPTIONS: Record<string, string> = {
  FAIL: 'Blocks the pipeline. Use for critical security requirements.',
  WARN: "Flags a concern but doesn't block. Use for best practices.",
  INFO: 'Informational only. Appears in reports but has no policy impact.',
  SKIP: 'Excludes matching components from further evaluation.',
}

const TARGET_DESCRIPTIONS: Record<string, string> = {
  ALL: 'Apply to all dependencies regardless of position in the dependency tree.',
  DIRECT: 'Only apply to dependencies directly declared in the project.',
  TRANSITIVE: 'Only apply to indirect/transitive dependencies.',
}

export default function RuleCard({ rule, onChange, onDelete }: RuleCardProps) {
  const handleFieldChange = (value: string) => {
    onChange({ ...rule, field: value as any })
  }

  const handleOperatorChange = (value: string) => {
    onChange({
      ...rule,
      operator: value as Rule['operator'],
      // Clear value if operator is EXISTS
      value: value === 'EXISTS' ? '' : rule.value,
    })
  }

  const handleValueChange = (value: string) => {
    onChange({ ...rule, value })
  }

  const handleSeverityChange = (value: string) => {
    onChange({ ...rule, severity: value as Rule['severity'] })
  }

  const handleTargetChange = (value: string) => {
    onChange({ ...rule, target: value as Rule['target'] })
  }

  const handleEcosystemChange = (value: string) => {
    onChange({ ...rule, ecosystem: value || undefined })
  }

  const handleIdChange = (value: string) => {
    onChange({ ...rule, id: value })
  }

  const handleDescriptionChange = (value: string) => {
    onChange({ ...rule, description: value })
  }

  return (
    <Card
      variant="outlined"
      sx={{
        bgcolor: 'rgba(0, 184, 212, 0.02)',
        borderColor: 'rgba(0, 184, 212, 0.1)',
      }}
    >
      <CardContent>
        <Stack spacing={2}>
          {/* Rule ID and Description */}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              label="Rule ID"
              value={rule.id}
              onChange={(e) => handleIdChange(e.target.value)}
              size="small"
              fullWidth
              helperText="Unique identifier for this rule (e.g. require-purl, min-scorecard). Used in findings reports."
              placeholder="e.g., require-purl"
            />
            <TextField
              label="Description"
              value={rule.description}
              onChange={(e) => handleDescriptionChange(e.target.value)}
              size="small"
              fullWidth
              helperText="Human-readable explanation of what this rule checks and why it matters."
              placeholder="e.g., Require package URL to be present"
            />
          </Stack>

          {/* Field and Operator */}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="flex-start">
            <Box sx={{ flex: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
                <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.primary' }}>
                  Field
                </Typography>
                <Tooltip title={FIELD_DESCRIPTIONS[rule.field] || 'Select a field to evaluate'}>
                  <InfoIcon sx={{ fontSize: '1rem', ml: 0.5, color: 'text.secondary' }} />
                </Tooltip>
              </Box>
              <Select
                value={rule.field}
                onChange={(e) => handleFieldChange(e.target.value)}
                fullWidth
                size="small"
              >
                {AVAILABLE_FIELDS.map((field) => (
                  <MenuItem key={field} value={field}>
                    {field}
                  </MenuItem>
                ))}
              </Select>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                The component attribute to evaluate. Different fields support different operators.
              </Typography>
            </Box>

            <Box sx={{ flex: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
                <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.primary' }}>
                  Operator
                </Typography>
                <Tooltip title={OPERATOR_DESCRIPTIONS[rule.operator] || ''}>
                  <InfoIcon sx={{ fontSize: '1rem', ml: 0.5, color: 'text.secondary' }} />
                </Tooltip>
              </Box>
              <Select
                value={rule.operator}
                onChange={(e) => handleOperatorChange(e.target.value)}
                fullWidth
                size="small"
              >
                {Object.keys(OPERATOR_DESCRIPTIONS).map((op) => (
                  <MenuItem key={op} value={op}>
                    {op}
                  </MenuItem>
                ))}
              </Select>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                {OPERATOR_DESCRIPTIONS[rule.operator]}
              </Typography>
            </Box>
          </Stack>

          {/* Value (disabled if EXISTS) */}
          <Box>
            <TextField
              label="Value"
              value={rule.value}
              onChange={(e) => handleValueChange(e.target.value)}
              disabled={rule.operator === 'EXISTS'}
              fullWidth
              size="small"
              helperText="The expected value to compare against. For IN/NOT_IN, use comma-separated values."
              placeholder="e.g., library or value1,value2,value3"
            />
          </Box>

          {/* Severity and Target */}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="flex-start">
            <Box sx={{ flex: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
                <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.primary' }}>
                  Severity
                </Typography>
                <Tooltip title={SEVERITY_DESCRIPTIONS[rule.severity] || ''}>
                  <InfoIcon sx={{ fontSize: '1rem', ml: 0.5, color: 'text.secondary' }} />
                </Tooltip>
              </Box>
              <Select
                value={rule.severity}
                onChange={(e) => handleSeverityChange(e.target.value)}
                fullWidth
                size="small"
              >
                {Object.entries(SEVERITY_DESCRIPTIONS).map(([sev]) => (
                  <MenuItem key={sev} value={sev}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Chip
                        label={sev}
                        size="small"
                        variant="filled"
                        color={
                          sev === 'FAIL'
                            ? 'error'
                            : sev === 'WARN'
                              ? 'warning'
                              : sev === 'INFO'
                                ? 'info'
                                : 'default'
                        }
                      />
                    </Box>
                  </MenuItem>
                ))}
              </Select>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                {SEVERITY_DESCRIPTIONS[rule.severity]}
              </Typography>
            </Box>

            <Box sx={{ flex: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
                <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.primary' }}>
                  Target
                </Typography>
                <Tooltip title={TARGET_DESCRIPTIONS[rule.target] || ''}>
                  <InfoIcon sx={{ fontSize: '1rem', ml: 0.5, color: 'text.secondary' }} />
                </Tooltip>
              </Box>
              <Select
                value={rule.target}
                onChange={(e) => handleTargetChange(e.target.value)}
                fullWidth
                size="small"
              >
                {Object.entries(TARGET_DESCRIPTIONS).map(([tgt]) => (
                  <MenuItem key={tgt} value={tgt}>
                    {tgt}
                  </MenuItem>
                ))}
              </Select>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                {TARGET_DESCRIPTIONS[rule.target]}
              </Typography>
            </Box>

            <Box sx={{ flex: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
                <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.primary' }}>
                  Ecosystem
                </Typography>
                <Tooltip title="Restrict this rule to a specific package ecosystem. Leave as 'All' to apply universally.">
                  <InfoIcon sx={{ fontSize: '1rem', ml: 0.5, color: 'text.secondary' }} />
                </Tooltip>
              </Box>
              <Select
                value={rule.ecosystem || ''}
                onChange={(e) => handleEcosystemChange(e.target.value)}
                fullWidth
                size="small"
              >
                {ECOSYSTEM_OPTIONS.map((opt) => (
                  <MenuItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </MenuItem>
                ))}
              </Select>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                {rule.ecosystem ? `Only applies to ${rule.ecosystem} packages` : 'Applies to all packages'}
              </Typography>
            </Box>

            {/* Delete Button */}
            <Box sx={{ mt: 1.5 }}>
              <Tooltip title="Delete this rule">
                <IconButton
                  size="small"
                  color="error"
                  onClick={onDelete}
                  sx={{ mt: 0.5 }}
                >
                  <DeleteIcon />
                </IconButton>
              </Tooltip>
            </Box>
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  )
}
