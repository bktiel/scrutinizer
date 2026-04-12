import { Rule } from '../components/RuleCard'

export interface PolicyForm {
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

export const INITIAL_FORM: PolicyForm = {
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

export const SCORING_METHOD_DESCRIPTIONS: Record<string, string> = {
  WEIGHTED_AVERAGE:
    'Score is computed as a weighted average of all rule results. Allows nuanced scoring where some rules matter more than others.',
  PASS_FAIL:
    'Binary scoring \u2014 the overall result is PASS if all rules pass, FAIL if any rule fails. No partial credit.',
  WORST_CASE:
    'Score is determined by the worst individual rule result. One failure sets the overall score.',
}

export function escapeYamlString(str: string): string {
  if (!str || typeof str !== 'string') return '""'
  if (/[:\n\r]/.test(str) || str.startsWith(' ') || str.endsWith(' ')) {
    return `"${str.replace(/"/g, '\\"')}"`
  }
  return str
}

export function generatePolicyYaml(form: PolicyForm): string {
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

export function parseYamlToForm(yamlString: string): PolicyForm | null {
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

      if (indent === 0) {
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
  } catch {
    return null
  }
}
