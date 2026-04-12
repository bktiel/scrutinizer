import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box,
  Button,
  Card,
  CardContent,
  Stack,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tooltip,
  IconButton,
  Select,
  MenuItem,
  Divider,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import InfoIcon from '@mui/icons-material/Info'
import EditIcon from '@mui/icons-material/Edit'
import RuleCard, { Rule } from '../components/RuleCard'
import PolicyYamlPreview from '../components/PolicyYamlPreview'
import { getPolicy, createPolicyFromYaml, updatePolicyFromYaml } from '../api/scrutinizerApi'

interface PolicyForm {
  name: string
  version: string
  description: string
  rules: Rule[]
  scoring: {
    method: 'WEIGHTED_AVERAGE' | 'PASS_FAIL' | 'WORST_CASE'
    passThreshold: number
    warnThreshold: number
  }
}

const INITIAL_FORM: PolicyForm = {
  name: '',
  version: '1.0',
  description: '',
  rules: [],
  scoring: {
    method: 'WEIGHTED_AVERAGE',
    passThreshold: 7.0,
    warnThreshold: 4.0,
  },
}

const SCORING_METHOD_DESCRIPTIONS: Record<string, string> = {
  WEIGHTED_AVERAGE:
    'Score is computed as a weighted average of all rule results. Allows nuanced scoring where some rules matter more than others.',
  PASS_FAIL:
    'Binary scoring — the overall result is PASS if all rules pass, FAIL if any rule fails. No partial credit.',
  WORST_CASE:
    'Score is determined by the worst individual rule result. One failure sets the overall score.',
}

function escapeYamlString(str: string): string {
  if (!str || typeof str !== 'string') return '""'
  if (/[:\n\r]/.test(str) || str.startsWith(' ') || str.endsWith(' ')) {
    return `"${str.replace(/"/g, '\\"')}"`
  }
  return str
}

function generatePolicyYaml(form: PolicyForm): string {
  const lines: string[] = []

  lines.push('apiVersion: scrutinizer/v1')
  lines.push('metadata:')
  lines.push(`  name: ${escapeYamlString(form.name)}`)
  lines.push(`  version: ${escapeYamlString(form.version)}`)
  lines.push(`  description: ${escapeYamlString(form.description)}`)

  lines.push('rules:')
  form.rules.forEach((rule) => {
    lines.push('  - id: ' + escapeYamlString(rule.id))
    lines.push(`    description: ${escapeYamlString(rule.description)}`)
    lines.push(`    field: ${rule.field}`)
    lines.push(`    operator: ${rule.operator}`)
    if (rule.operator !== 'EXISTS') {
      lines.push(`    value: ${escapeYamlString(rule.value)}`)
    }
    lines.push(`    severity: ${rule.severity}`)
    lines.push(`    target: ${rule.target}`)
  })

  lines.push('scoring:')
  lines.push(`  method: ${form.scoring.method}`)
  lines.push(`  passThreshold: ${form.scoring.passThreshold}`)
  lines.push(`  warnThreshold: ${form.scoring.warnThreshold}`)

  return lines.join('\n')
}

function parseYamlToForm(yamlString: string): PolicyForm | null {
  try {
    const lines = yamlString.split('\n')
    const form: Partial<PolicyForm> = { ...INITIAL_FORM }
    const rules: Rule[] = []

    let currentRule: Partial<Rule> | null = null
    let section = ''

    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed || trimmed.startsWith('#')) continue

      const indent = line.search(/\S/)
      const colonIdx = trimmed.indexOf(':')

      if (indent === 0) {
        // Top-level keys
        if (trimmed.startsWith('apiVersion:')) continue
        if (trimmed.startsWith('metadata:')) {
          section = 'metadata'
          continue
        }
        if (trimmed.startsWith('rules:')) {
          section = 'rules'
          continue
        }
        if (trimmed.startsWith('scoring:')) {
          section = 'scoring'
          continue
        }
      } else if (indent === 2) {
        // Metadata level or rules item or scoring level
        if (section === 'metadata') {
          const [key, ...valueParts] = trimmed.split(':')
          const value = valueParts.join(':').trim().replace(/^["']|["']$/g, '')
          if (key === 'name') form.name = value
          if (key === 'version') form.version = value
          if (key === 'description') form.description = value
        } else if (section === 'scoring') {
          const [key, ...valueParts] = trimmed.split(':')
          const value = valueParts.join(':').trim()
          if (key === 'method') form.scoring = { ...form.scoring!, method: value as any }
          if (key === 'passThreshold') form.scoring = { ...form.scoring!, passThreshold: parseFloat(value) }
          if (key === 'warnThreshold') form.scoring = { ...form.scoring!, warnThreshold: parseFloat(value) }
        } else if (section === 'rules') {
          if (trimmed.startsWith('- id:')) {
            if (currentRule) rules.push(currentRule as Rule)
            const value = trimmed.substring(5).trim().replace(/^["']|["']$/g, '')
            currentRule = { id: value, field: 'name', operator: 'EQ', value: '', severity: 'FAIL', target: 'ALL' }
          }
        }
      } else if (indent === 4 && section === 'rules' && currentRule) {
        // Rule fields
        const [key, ...valueParts] = trimmed.split(':')
        const value = valueParts.join(':').trim().replace(/^["']|["']$/g, '')
        if (key === 'description') currentRule.description = value
        if (key === 'field') currentRule.field = value
        if (key === 'operator') currentRule.operator = value as any
        if (key === 'value') currentRule.value = value
        if (key === 'severity') currentRule.severity = value as any
        if (key === 'target') currentRule.target = value as any
      }
    }

    if (currentRule) rules.push(currentRule as Rule)

    if (!form.name) return null

    return {
      name: form.name || '',
      version: form.version || '1.0',
      description: form.description || '',
      rules,
      scoring: form.scoring || INITIAL_FORM.scoring,
    }
  } catch (e) {
    return null
  }
}

export default function PolicyConfiguratorPage() {
  const { id } = useParams<{ id?: string }>()
  const navigate = useNavigate()
  const isEditing = id && id !== 'new'

  const [form, setForm] = useState<PolicyForm>(INITIAL_FORM)
  const [loading, setLoading] = useState(!!isEditing)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [previewOpen, setPreviewOpen] = useState(false)
  const [importDialogOpen, setImportDialogOpen] = useState(false)
  const [importYaml, setImportYaml] = useState('')
  const [importError, setImportError] = useState<string | null>(null)

  // Load existing policy if editing
  useEffect(() => {
    if (isEditing) {
      getPolicy(id!)
        .then((policy) => {
          const parsed = parseYamlToForm(policy.policyYaml)
          if (parsed) {
            setForm(parsed)
          } else {
            setError('Failed to parse policy YAML')
          }
        })
        .catch((e) => setError(e.message))
        .finally(() => setLoading(false))
    }
  }, [id, isEditing])

  const addRule = useCallback(() => {
    const newRule: Rule = {
      id: `rule-${Date.now()}`,
      description: '',
      field: 'name',
      operator: 'EQ',
      value: '',
      severity: 'FAIL',
      target: 'ALL',
    }
    setForm({ ...form, rules: [...form.rules, newRule] })
  }, [form])

  const updateRule = useCallback(
    (index: number, updated: Rule) => {
      const newRules = [...form.rules]
      newRules[index] = updated
      setForm({ ...form, rules: newRules })
    },
    [form]
  )

  const deleteRule = useCallback(
    (index: number) => {
      if (!confirm('Delete this rule?')) return
      setForm({ ...form, rules: form.rules.filter((_, i) => i !== index) })
    },
    [form]
  )

  const handleSave = async () => {
    if (!form.name.trim()) {
      setError('Policy name is required')
      return
    }

    if (form.rules.length === 0) {
      setError('At least one rule is required')
      return
    }

    try {
      setSaving(true)
      setError(null)
      const policyYaml = generatePolicyYaml(form)

      if (isEditing) {
        await updatePolicyFromYaml(id!, policyYaml, form.description || undefined)
      } else {
        await createPolicyFromYaml(policyYaml, form.description || undefined)
      }

      navigate('/policies')
    } catch (e: any) {
      setError(e.message || 'Failed to save policy')
    } finally {
      setSaving(false)
    }
  }

  const handleImportYaml = () => {
    setImportError(null)
    const parsed = parseYamlToForm(importYaml)
    if (parsed) {
      setForm(parsed)
      setImportDialogOpen(false)
      setImportYaml('')
    } else {
      setImportError('Invalid YAML format. Please check the policy structure.')
    }
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box>
      {/* Header */}
      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        sx={{ mb: 3 }}
      >
        <Typography variant="h4">
          {isEditing ? 'Edit Policy' : 'Create Policy'}
        </Typography>
        <Button variant="text" onClick={() => navigate('/policies')}>
          Back to Policies
        </Button>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Metadata Section */}
      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
            Policy Metadata
          </Typography>

          <Stack spacing={2}>
            <TextField
              label="Policy Name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              fullWidth
              helperText="Unique identifier for this policy. Used to reference the policy in CI pipelines and API calls."
              placeholder="e.g., default-security-policy"
              required
            />

            <TextField
              label="Version"
              value={form.version}
              onChange={(e) => setForm({ ...form, version: e.target.value })}
              fullWidth
              helperText="Semantic version string (e.g. 1.0, 2.1). Increment when making changes to track policy evolution."
              placeholder="e.g., 1.0"
            />

            <TextField
              label="Description"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              fullWidth
              multiline
              rows={3}
              helperText="Human-readable description of what this policy enforces and its intended use case."
              placeholder="e.g., This policy enforces minimum security standards for production dependencies..."
            />
          </Stack>
        </CardContent>
      </Card>

      {/* Rules Section */}
      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">Rules</Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={addRule}
              size="small"
            >
              Add Rule
            </Button>
          </Box>

          <Stack spacing={2}>
            {form.rules.length === 0 ? (
              <Typography color="text.secondary" sx={{ fontStyle: 'italic' }}>
                No rules yet. Click "Add Rule" to create your first rule.
              </Typography>
            ) : (
              form.rules.map((rule, index) => (
                <RuleCard
                  key={rule.id}
                  rule={rule}
                  onChange={(updated) => updateRule(index, updated)}
                  onDelete={() => deleteRule(index)}
                />
              ))
            )}
          </Stack>
        </CardContent>
      </Card>

      {/* Scoring Section */}
      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Scoring Configuration
          </Typography>

          <Stack spacing={2}>
            {/* Scoring Method */}
            <Box>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
                <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.primary' }}>
                  Scoring Method
                </Typography>
                <Tooltip title={SCORING_METHOD_DESCRIPTIONS['WEIGHTED_AVERAGE']}>
                  <InfoIcon sx={{ fontSize: '1rem', ml: 0.5, color: 'text.secondary' }} />
                </Tooltip>
              </Box>
              <Select
                value={form.scoring.method}
                onChange={(e) =>
                  setForm({
                    ...form,
                    scoring: {
                      ...form.scoring,
                      method: e.target.value as 'WEIGHTED_AVERAGE' | 'PASS_FAIL' | 'WORST_CASE',
                    },
                  })
                }
                fullWidth
                size="small"
              >
                {Object.entries(SCORING_METHOD_DESCRIPTIONS).map(([method, desc]) => (
                  <MenuItem key={method} value={method}>
                    {method}
                  </MenuItem>
                ))}
              </Select>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                {SCORING_METHOD_DESCRIPTIONS[form.scoring.method]}
              </Typography>
            </Box>

            <Divider />

            {/* Pass Threshold */}
            <Box>
              <TextField
                label="Pass Threshold"
                type="number"
                value={form.scoring.passThreshold}
                onChange={(e) =>
                  setForm({
                    ...form,
                    scoring: {
                      ...form.scoring,
                      passThreshold: parseFloat(e.target.value) || 0,
                    },
                  })
                }
                inputProps={{ min: 0, max: 10, step: 0.1 }}
                helperText="Score at or above this value results in PASS decision. Default: 7.0"
                fullWidth
                size="small"
              />
            </Box>

            {/* Warn Threshold */}
            <Box>
              <TextField
                label="Warn Threshold"
                type="number"
                value={form.scoring.warnThreshold}
                onChange={(e) =>
                  setForm({
                    ...form,
                    scoring: {
                      ...form.scoring,
                      warnThreshold: parseFloat(e.target.value) || 0,
                    },
                  })
                }
                inputProps={{ min: 0, max: 10, step: 0.1 }}
                helperText="Score between this and the pass threshold results in WARN. Below this is FAIL. Default: 4.0"
                fullWidth
                size="small"
              />
            </Box>
          </Stack>
        </CardContent>
      </Card>

      {/* Actions */}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 3 }}>
        <Button
          variant="outlined"
          onClick={() => setPreviewOpen(true)}
          disabled={!form.name}
        >
          Preview YAML
        </Button>

        <Button
          variant="outlined"
          onClick={() => setImportDialogOpen(true)}
          startIcon={<EditIcon />}
        >
          Import YAML
        </Button>

        <Box sx={{ flexGrow: 1 }} />

        <Button
          variant="text"
          onClick={() => navigate('/policies')}
        >
          Cancel
        </Button>

        <Button
          variant="contained"
          onClick={handleSave}
          disabled={!form.name || form.rules.length === 0 || saving}
        >
          {saving ? <CircularProgress size={24} /> : isEditing ? 'Update Policy' : 'Create Policy'}
        </Button>
      </Stack>

      {/* YAML Preview Dialog */}
      <PolicyYamlPreview
        open={previewOpen}
        yaml={generatePolicyYaml(form)}
        onClose={() => setPreviewOpen(false)}
      />

      {/* Import YAML Dialog */}
      <Dialog open={importDialogOpen} onClose={() => setImportDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Import Policy YAML</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Policy YAML"
              value={importYaml}
              onChange={(e) => setImportYaml(e.target.value)}
              fullWidth
              multiline
              rows={10}
              placeholder="Paste your policy YAML here..."
              helperText="Paste a complete policy YAML file to populate the form."
            />
            {importError && (
              <Alert severity="error">{importError}</Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setImportDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleImportYaml}
            disabled={!importYaml.trim()}
          >
            Import
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
