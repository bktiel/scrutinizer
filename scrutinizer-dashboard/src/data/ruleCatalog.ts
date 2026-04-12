import type { Rule } from '../components/RuleCard'

export interface CatalogParam {
  key: string
  label: string
  type: 'number' | 'text' | 'text-list'
  defaultValue: string
  placeholder?: string
  helperText?: string
}

export interface CatalogEntry {
  id: string
  name: string
  description: string
  category: 'security' | 'provenance' | 'package-ban' | 'metadata'
  /** Human-readable sentence with {paramKey} placeholders */
  sentence: string
  parameters: CatalogParam[]
  toRule: (params: Record<string, string>) => Rule
}

export const CATEGORY_LABELS: Record<string, string> = {
  security: 'Security',
  provenance: 'Provenance & Supply Chain',
  'package-ban': 'Package Management',
  metadata: 'Metadata & Filtering',
}

export const RULE_CATALOG: CatalogEntry[] = [
  {
    id: 'scorecard-minimum',
    name: 'Minimum Scorecard Score',
    description: 'Require a minimum OpenSSF Scorecard score for all dependencies.',
    category: 'security',
    sentence: 'Require a minimum OpenSSF Scorecard score of {threshold}',
    parameters: [
      { key: 'threshold', label: 'Minimum score', type: 'number', defaultValue: '4.0', helperText: 'Score from 0 to 10' },
    ],
    toRule: (params) => ({
      id: 'scorecard-minimum',
      description: 'Require a minimum OpenSSF Scorecard score',
      field: 'scorecard.score',
      operator: 'GTE',
      value: params.threshold || '4.0',
      severity: 'WARN',
      target: 'ALL',
    }),
  },
  {
    id: 'require-provenance',
    name: 'Require Provenance Attestation',
    description: 'Require SLSA/Sigstore provenance attestation on packages.',
    category: 'provenance',
    sentence: 'Require SLSA provenance attestation on all packages',
    parameters: [],
    toRule: () => ({
      id: 'require-provenance',
      description: 'Require SLSA provenance attestation',
      field: 'provenance.present',
      operator: 'EQ',
      value: 'true',
      severity: 'WARN',
      target: 'ALL',
    }),
  },
  {
    id: 'require-purl',
    name: 'Require Package URL',
    description: 'Require a Package URL (purl) for traceability and identification.',
    category: 'provenance',
    sentence: 'Require a Package URL (purl) for traceability',
    parameters: [],
    toRule: () => ({
      id: 'require-purl',
      description: 'Require a Package URL for traceability',
      field: 'purl',
      operator: 'EXISTS',
      value: '',
      severity: 'WARN',
      target: 'ALL',
    }),
  },
  {
    id: 'ban-package',
    name: 'Ban Specific Package',
    description: 'Block a known-bad or deprecated package by name.',
    category: 'package-ban',
    sentence: 'Block package named {packageName}',
    parameters: [
      { key: 'packageName', label: 'Package name', type: 'text', defaultValue: '', placeholder: 'e.g. event-stream' },
    ],
    toRule: (params) => ({
      id: `ban-${params.packageName || 'package'}`,
      description: `Block ${params.packageName || 'package'}`,
      field: 'name',
      operator: 'EQ',
      value: params.packageName || '',
      severity: 'FAIL',
      target: 'ALL',
    }),
  },
  {
    id: 'ban-packages',
    name: 'Ban Multiple Packages',
    description: 'Block a list of known-bad or deprecated packages.',
    category: 'package-ban',
    sentence: 'Block any of these packages: {packageNames}',
    parameters: [
      { key: 'packageNames', label: 'Package names', type: 'text-list', defaultValue: '', placeholder: 'e.g. event-stream, colors, faker', helperText: 'Comma-separated list' },
    ],
    toRule: (params) => ({
      id: 'ban-packages',
      description: 'Block known-bad packages',
      field: 'name',
      operator: 'IN',
      value: params.packageNames || '',
      severity: 'FAIL',
      target: 'ALL',
    }),
  },
  {
    id: 'restrict-types',
    name: 'Restrict Component Types',
    description: 'Only allow specific component types (e.g. library, framework).',
    category: 'metadata',
    sentence: 'Only allow components of type: {allowedTypes}',
    parameters: [
      { key: 'allowedTypes', label: 'Allowed types', type: 'text-list', defaultValue: 'library,framework,application', placeholder: 'e.g. library, framework' },
    ],
    toRule: (params) => ({
      id: 'valid-types',
      description: 'Restrict to allowed component types',
      field: 'type',
      operator: 'IN',
      value: params.allowedTypes || 'library,framework,application',
      severity: 'FAIL',
      target: 'ALL',
    }),
  },
  {
    id: 'flag-deprecated',
    name: 'Flag Deprecated Package',
    description: 'Warn when a specific deprecated package is used.',
    category: 'package-ban',
    sentence: 'Warn when package {packageName} is used',
    parameters: [
      { key: 'packageName', label: 'Package name', type: 'text', defaultValue: '', placeholder: 'e.g. moment' },
    ],
    toRule: (params) => ({
      id: `warn-${params.packageName || 'package'}`,
      description: `Warn about deprecated package: ${params.packageName || 'package'}`,
      field: 'name',
      operator: 'EQ',
      value: params.packageName || '',
      severity: 'WARN',
      target: 'ALL',
    }),
  },
  {
    id: 'require-group',
    name: 'Require Specific Group',
    description: 'Require packages belong to a specific group or organization.',
    category: 'metadata',
    sentence: 'Require packages belong to group {group}',
    parameters: [
      { key: 'group', label: 'Group name', type: 'text', defaultValue: '', placeholder: 'e.g. org.apache' },
    ],
    toRule: (params) => ({
      id: 'require-group',
      description: `Require group: ${params.group || ''}`,
      field: 'group',
      operator: 'EQ',
      value: params.group || '',
      severity: 'WARN',
      target: 'ALL',
    }),
  },
  {
    id: 'skip-scope',
    name: 'Exclude by Scope',
    description: 'Exclude dependencies with a specific scope from evaluation.',
    category: 'metadata',
    sentence: 'Exclude {scope} dependencies from checks',
    parameters: [
      { key: 'scope', label: 'Scope', type: 'text', defaultValue: 'optional', placeholder: 'e.g. optional, test' },
    ],
    toRule: (params) => ({
      id: `skip-${params.scope || 'scope'}`,
      description: `Skip ${params.scope || 'optional'} dependencies`,
      field: 'scope',
      operator: 'EQ',
      value: params.scope || 'optional',
      severity: 'SKIP',
      target: 'ALL',
    }),
  },
]

/** Attempt to match an existing Rule back to a catalog entry */
export function matchRuleToCatalog(rule: Rule): CatalogEntry | null {
  // Match by (field, operator) signature
  if (rule.field === 'scorecard.score' && rule.operator === 'GTE') return RULE_CATALOG.find((c) => c.id === 'scorecard-minimum') || null
  if (rule.field === 'provenance.present' && rule.operator === 'EQ') return RULE_CATALOG.find((c) => c.id === 'require-provenance') || null
  if (rule.field === 'purl' && rule.operator === 'EXISTS') return RULE_CATALOG.find((c) => c.id === 'require-purl') || null
  if (rule.field === 'name' && rule.operator === 'EQ' && rule.severity === 'FAIL') return RULE_CATALOG.find((c) => c.id === 'ban-package') || null
  if (rule.field === 'name' && rule.operator === 'IN' && rule.severity === 'FAIL') return RULE_CATALOG.find((c) => c.id === 'ban-packages') || null
  if (rule.field === 'name' && rule.operator === 'EQ' && rule.severity === 'WARN') return RULE_CATALOG.find((c) => c.id === 'flag-deprecated') || null
  if (rule.field === 'type' && rule.operator === 'IN') return RULE_CATALOG.find((c) => c.id === 'restrict-types') || null
  if (rule.field === 'group' && rule.operator === 'EQ') return RULE_CATALOG.find((c) => c.id === 'require-group') || null
  if (rule.field === 'scope' && rule.operator === 'EQ' && rule.severity === 'SKIP') return RULE_CATALOG.find((c) => c.id === 'skip-scope') || null
  return null
}

/** Extract parameter values from an existing Rule for a matched catalog entry */
export function extractParamsFromRule(rule: Rule, entry: CatalogEntry): Record<string, string> {
  const params: Record<string, string> = {}
  for (const p of entry.parameters) {
    if (p.key === 'threshold') params[p.key] = rule.value
    else if (p.key === 'packageName') params[p.key] = rule.value
    else if (p.key === 'packageNames') params[p.key] = rule.value
    else if (p.key === 'allowedTypes') params[p.key] = rule.value
    else if (p.key === 'group') params[p.key] = rule.value
    else if (p.key === 'scope') params[p.key] = rule.value
    else params[p.key] = rule.value
  }
  return params
}
