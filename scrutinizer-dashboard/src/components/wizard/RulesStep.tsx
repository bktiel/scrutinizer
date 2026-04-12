import { useState } from 'react'
import { Box, Button, Stack, Typography } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import TuneIcon from '@mui/icons-material/Tune'
import type { Rule } from '../RuleCard'
import RuleCard from '../RuleCard'
import CatalogRuleCard from './CatalogRuleCard'
import RuleCatalogDialog from './RuleCatalogDialog'
import type { PolicyForm } from '../../utils/policyYaml'
import { matchRuleToCatalog, CatalogEntry } from '../../data/ruleCatalog'

interface Props {
  form: PolicyForm
  onChange: (form: PolicyForm) => void
}

export default function RulesStep({ form, onChange }: Props) {
  const [catalogOpen, setCatalogOpen] = useState(false)

  const addCatalogRule = (entry: CatalogEntry) => {
    const defaults: Record<string, string> = {}
    for (const p of entry.parameters) {
      defaults[p.key] = p.defaultValue
    }
    const rule = entry.toRule(defaults)
    // If a rule with this ID already exists, suffix with timestamp
    const existingIds = new Set(form.rules.map((r) => r.id))
    if (existingIds.has(rule.id)) {
      rule.id = `${rule.id}-${Date.now()}`
    }
    onChange({ ...form, rules: [...form.rules, rule] })
  }

  const addCustomRule = () => {
    const newRule: Rule = {
      id: `custom-${Date.now()}`,
      description: '',
      field: 'name',
      operator: 'EQ',
      value: '',
      severity: 'FAIL',
      target: 'ALL',
    }
    onChange({ ...form, rules: [...form.rules, newRule] })
  }

  const updateRule = (index: number, updated: Rule) => {
    const newRules = [...form.rules]
    newRules[index] = updated
    onChange({ ...form, rules: newRules })
  }

  const deleteRule = (index: number) => {
    onChange({ ...form, rules: form.rules.filter((_, i) => i !== index) })
  }

  return (
    <Box>
      <Typography variant="h6" sx={{ mb: 1 }}>
        Define Rules
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Add rules that define what your policy checks. Choose from the catalog for common rules, or add a custom rule for advanced use cases.
      </Typography>

      <Stack direction="row" spacing={1} sx={{ mb: 3 }}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCatalogOpen(true)}>
          Add from Catalog
        </Button>
        <Button variant="outlined" startIcon={<TuneIcon />} onClick={addCustomRule}>
          Add Custom Rule
        </Button>
      </Stack>

      <Stack spacing={2}>
        {form.rules.length === 0 ? (
          <Box
            sx={{
              border: '1px dashed rgba(139, 148, 158, 0.3)',
              borderRadius: 2,
              p: 4,
              textAlign: 'center',
            }}
          >
            <Typography color="text.secondary" sx={{ mb: 1 }}>
              No rules yet
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Click "Add from Catalog" to get started with pre-built rules, or "Add Custom Rule" for full control.
            </Typography>
          </Box>
        ) : (
          form.rules.map((rule, index) => {
            const catalogEntry = matchRuleToCatalog(rule)
            if (catalogEntry) {
              return (
                <CatalogRuleCard
                  key={`${rule.id}-${index}`}
                  rule={rule}
                  catalogEntry={catalogEntry}
                  onChange={(updated) => updateRule(index, updated)}
                  onDelete={() => deleteRule(index)}
                />
              )
            }
            return (
              <RuleCard
                key={`${rule.id}-${index}`}
                rule={rule}
                onChange={(updated) => updateRule(index, updated)}
                onDelete={() => deleteRule(index)}
              />
            )
          })
        )}
      </Stack>

      <RuleCatalogDialog open={catalogOpen} onClose={() => setCatalogOpen(false)} onSelect={addCatalogRule} />
    </Box>
  )
}
