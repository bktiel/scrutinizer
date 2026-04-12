import type { PolicyForm } from '../utils/policyYaml'

export interface PolicyTemplate {
  id: string
  name: string
  description: string
  iconLabel: string
  accentColor: string
  form: PolicyForm
  severitySummary: { FAIL: number; WARN: number; INFO: number; SKIP: number }
  dashed?: boolean
}

export const POLICY_TEMPLATES: PolicyTemplate[] = [
  {
    id: 'balanced',
    name: 'Balanced',
    description: 'Sensible defaults that block known-bad packages and warn on missing provenance.',
    iconLabel: 'Recommended',
    accentColor: '#00B8D4',
    severitySummary: { FAIL: 3, WARN: 3, INFO: 0, SKIP: 0 },
    form: {
      name: 'balanced-policy',
      version: '1.0',
      description: 'Balanced security policy with sensible defaults for production dependencies.',
      rules: [
        { id: 'scorecard-minimum', description: 'Require a minimum OpenSSF Scorecard score', field: 'scorecard.score', operator: 'GTE', value: '4.0', severity: 'WARN', target: 'ALL' },
        { id: 'require-provenance', description: 'Check for SLSA provenance attestation', field: 'provenance.present', operator: 'EQ', value: 'true', severity: 'WARN', target: 'ALL' },
        { id: 'require-purl', description: 'Require a Package URL for traceability', field: 'purl', operator: 'EXISTS', value: '', severity: 'WARN', target: 'ALL' },
        { id: 'ban-event-stream', description: 'Block event-stream (known compromised)', field: 'name', operator: 'EQ', value: 'event-stream', severity: 'FAIL', target: 'ALL' },
        { id: 'ban-colors', description: 'Block colors (known sabotaged)', field: 'name', operator: 'EQ', value: 'colors', severity: 'FAIL', target: 'ALL' },
        { id: 'ban-faker', description: 'Block faker (known sabotaged)', field: 'name', operator: 'EQ', value: 'faker', severity: 'FAIL', target: 'ALL' },
      ],
      scoring: { method: 'WEIGHTED_AVERAGE', passThreshold: 7.0, warnThreshold: 4.0 },
    },
  },
  {
    id: 'strict',
    name: 'Strict Compliance',
    description: 'High-bar policy that blocks on any failure. Best for regulated environments.',
    iconLabel: 'Strict',
    accentColor: '#FF5252',
    severitySummary: { FAIL: 4, WARN: 0, INFO: 0, SKIP: 0 },
    form: {
      name: 'strict-compliance',
      version: '1.0',
      description: 'Strict compliance policy that blocks on any rule failure.',
      rules: [
        { id: 'scorecard-high', description: 'Require high OpenSSF Scorecard score', field: 'scorecard.score', operator: 'GTE', value: '7.0', severity: 'FAIL', target: 'ALL' },
        { id: 'provenance-required', description: 'Require SLSA provenance attestation', field: 'provenance.present', operator: 'EQ', value: 'true', severity: 'FAIL', target: 'ALL' },
        { id: 'require-purl', description: 'Require a Package URL for traceability', field: 'purl', operator: 'EXISTS', value: '', severity: 'FAIL', target: 'ALL' },
        { id: 'valid-types', description: 'Only allow known component types', field: 'type', operator: 'IN', value: 'library,framework,application', severity: 'FAIL', target: 'ALL' },
      ],
      scoring: { method: 'WORST_CASE', passThreshold: 7.0, warnThreshold: 4.0 },
    },
  },
  {
    id: 'permissive',
    name: 'Monitor Only',
    description: 'Observe-only policy that warns but never blocks. Great for getting started.',
    iconLabel: 'Observe',
    accentColor: '#00E676',
    severitySummary: { FAIL: 0, WARN: 1, INFO: 1, SKIP: 0 },
    form: {
      name: 'monitor-only',
      version: '1.0',
      description: 'Observe-only policy that reports findings without blocking.',
      rules: [
        { id: 'scorecard-advisory', description: 'Advise on low Scorecard scores', field: 'scorecard.score', operator: 'GTE', value: '3.0', severity: 'WARN', target: 'ALL' },
        { id: 'provenance-advisory', description: 'Inform about missing provenance', field: 'provenance.present', operator: 'EQ', value: 'true', severity: 'INFO', target: 'ALL' },
      ],
      scoring: { method: 'PASS_FAIL', passThreshold: 3.0, warnThreshold: 1.0 },
    },
  },
  {
    id: 'scratch',
    name: 'Start from Scratch',
    description: 'Begin with a blank policy and add your own rules.',
    iconLabel: 'Custom',
    accentColor: '#8B949E',
    severitySummary: { FAIL: 0, WARN: 0, INFO: 0, SKIP: 0 },
    dashed: true,
    form: {
      name: '',
      version: '1.0',
      description: '',
      rules: [],
      scoring: { method: 'WEIGHTED_AVERAGE', passThreshold: 7.0, warnThreshold: 4.0 },
    },
  },
]
