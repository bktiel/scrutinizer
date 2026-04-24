import { describe, it, expect } from 'vitest'
import {
  escapeYamlString,
  generatePolicyYaml,
  parseYamlToForm,
  INITIAL_FORM,
  SCORING_METHOD_DESCRIPTIONS,
} from '../../utils/policyYaml'
import type { PolicyForm } from '../../utils/policyYaml'

describe('policyYaml utilities', () => {
  describe('escapeYamlString', () => {
    it('returns plain string unchanged', () => {
      expect(escapeYamlString('hello')).toBe('hello')
    })

    it('wraps string containing colon in quotes', () => {
      expect(escapeYamlString('key: value')).toBe('"key: value"')
    })

    it('wraps string containing newline in quotes', () => {
      expect(escapeYamlString('line1\nline2')).toBe('"line1\nline2"')
    })

    it('wraps string starting with space in quotes', () => {
      expect(escapeYamlString(' leading')).toBe('" leading"')
    })

    it('wraps string ending with space in quotes', () => {
      expect(escapeYamlString('trailing ')).toBe('"trailing "')
    })

    it('escapes internal double quotes', () => {
      expect(escapeYamlString('say "hello"')).toBe('say "hello"')
    })

    it('escapes quotes inside coloned strings', () => {
      expect(escapeYamlString('key: "val"')).toBe('"key: \\"val\\""')
    })

    it('returns empty quotes for null/empty/non-string', () => {
      expect(escapeYamlString('')).toBe('""')
      expect(escapeYamlString(null as any)).toBe('""')
      expect(escapeYamlString(undefined as any)).toBe('""')
    })
  })

  describe('generatePolicyYaml', () => {
    it('generates basic structure with no rules', () => {
      const form: PolicyForm = {
        name: 'test-policy',
        version: '1.0',
        description: 'A test',
        rules: [],
        scoring: { method: 'WEIGHTED_AVERAGE', passThreshold: 7.0, warnThreshold: 4.0 },
      }

      const yaml = generatePolicyYaml(form)
      expect(yaml).toContain('apiVersion: scrutinizer/v1')
      expect(yaml).toContain('  name: test-policy')
      expect(yaml).toContain('  version: 1.0')
      expect(yaml).toContain('  description: A test')
      expect(yaml).toContain('rules:')
      expect(yaml).toContain('scoring:')
      expect(yaml).toContain('  method: WEIGHTED_AVERAGE')
      expect(yaml).toContain('  passThreshold: 7')
      expect(yaml).toContain('  warnThreshold: 4')
    })

    it('generates rules with all fields', () => {
      const form: PolicyForm = {
        name: 'with-rules',
        version: '1.0',
        description: '',
        rules: [
          {
            id: 'scorecard-min',
            description: 'Minimum scorecard score',
            field: 'scorecard.score',
            operator: 'GTE',
            value: '5.0',
            severity: 'FAIL',
            target: 'ALL',
          },
        ],
        scoring: { method: 'PASS_FAIL', passThreshold: 7.0, warnThreshold: 4.0 },
      }

      const yaml = generatePolicyYaml(form)
      expect(yaml).toContain('  - id: scorecard-min')
      expect(yaml).toContain('    field: scorecard.score')
      expect(yaml).toContain('    operator: GTE')
      expect(yaml).toContain('    value: 5.0')
      expect(yaml).toContain('    severity: FAIL')
      expect(yaml).toContain('    target: ALL')
    })

    it('omits value for EXISTS operator', () => {
      const form: PolicyForm = {
        name: 'exists-rule',
        version: '1.0',
        description: '',
        rules: [
          {
            id: 'has-provenance',
            description: 'Provenance must exist',
            field: 'provenance.present',
            operator: 'EXISTS',
            value: '',
            severity: 'WARN',
            target: 'DIRECT',
          },
        ],
        scoring: { method: 'WEIGHTED_AVERAGE', passThreshold: 7.0, warnThreshold: 4.0 },
      }

      const yaml = generatePolicyYaml(form)
      expect(yaml).toContain('    operator: EXISTS')
      expect(yaml).not.toContain('    value:')
    })

    it('includes ecosystem when present', () => {
      const form: PolicyForm = {
        name: 'eco-rule',
        version: '1.0',
        description: '',
        rules: [
          {
            id: 'npm-only',
            description: 'npm specific',
            field: 'name',
            operator: 'EQ',
            value: 'test',
            severity: 'FAIL',
            target: 'ALL',
            ecosystem: 'npm',
          },
        ],
        scoring: { method: 'WEIGHTED_AVERAGE', passThreshold: 7.0, warnThreshold: 4.0 },
      }

      const yaml = generatePolicyYaml(form)
      expect(yaml).toContain('    ecosystem: npm')
    })
  })

  describe('parseYamlToForm', () => {
    it('round-trips a generated YAML back to form', () => {
      const original: PolicyForm = {
        name: 'test-policy',
        version: '2.0',
        description: 'A test policy',
        rules: [
          {
            id: 'rule-1',
            description: 'First rule',
            field: 'scorecard.score',
            operator: 'GTE',
            value: '5.0',
            severity: 'FAIL',
            target: 'ALL',
          },
        ],
        scoring: { method: 'PASS_FAIL', passThreshold: 8.0, warnThreshold: 3.0 },
      }

      const yaml = generatePolicyYaml(original)
      const parsed = parseYamlToForm(yaml)

      expect(parsed).not.toBeNull()
      expect(parsed!.name).toBe('test-policy')
      expect(parsed!.version).toBe('2.0')
      expect(parsed!.description).toBe('A test policy')
      expect(parsed!.rules).toHaveLength(1)
      expect(parsed!.rules[0].id).toBe('rule-1')
      expect(parsed!.rules[0].field).toBe('scorecard.score')
      expect(parsed!.rules[0].operator).toBe('GTE')
      expect(parsed!.scoring.method).toBe('PASS_FAIL')
      expect(parsed!.scoring.passThreshold).toBe(8.0)
      expect(parsed!.scoring.warnThreshold).toBe(3.0)
    })

    it('parses multiple rules', () => {
      const form: PolicyForm = {
        name: 'multi',
        version: '1.0',
        description: '',
        rules: [
          { id: 'r1', description: 'Rule 1', field: 'name', operator: 'EQ', value: 'a', severity: 'FAIL', target: 'ALL' },
          { id: 'r2', description: 'Rule 2', field: 'version', operator: 'NEQ', value: 'b', severity: 'WARN', target: 'DIRECT' },
        ],
        scoring: { method: 'WEIGHTED_AVERAGE', passThreshold: 7.0, warnThreshold: 4.0 },
      }

      const yaml = generatePolicyYaml(form)
      const parsed = parseYamlToForm(yaml)

      expect(parsed!.rules).toHaveLength(2)
      expect(parsed!.rules[0].id).toBe('r1')
      expect(parsed!.rules[1].id).toBe('r2')
    })

    it('returns null for empty name', () => {
      const yaml = 'apiVersion: scrutinizer/v1\nmetadata:\n  version: "1.0"\nrules:\nscoring:\n  method: PASS_FAIL'
      expect(parseYamlToForm(yaml)).toBeNull()
    })

    it('returns null for invalid YAML', () => {
      expect(parseYamlToForm('')).toBeNull()
    })

    it('ignores comment lines', () => {
      const yaml = '# comment\napiVersion: scrutinizer/v1\nmetadata:\n  name: test\n  version: "1.0"\nrules:\nscoring:\n  method: PASS_FAIL'
      const parsed = parseYamlToForm(yaml)
      expect(parsed).not.toBeNull()
      expect(parsed!.name).toBe('test')
    })

    it('handles ecosystem field in rules', () => {
      const yaml = `apiVersion: scrutinizer/v1
metadata:
  name: eco-test
  version: "1.0"
rules:
  - id: npm-rule
    description: npm specific
    field: name
    operator: EQ
    value: test
    severity: FAIL
    target: ALL
    ecosystem: npm
scoring:
  method: PASS_FAIL`

      const parsed = parseYamlToForm(yaml)
      expect(parsed!.rules[0].ecosystem).toBe('npm')
    })
  })

  describe('constants', () => {
    it('INITIAL_FORM has expected defaults', () => {
      expect(INITIAL_FORM.name).toBe('')
      expect(INITIAL_FORM.version).toBe('1.0')
      expect(INITIAL_FORM.rules).toHaveLength(0)
      expect(INITIAL_FORM.scoring.method).toBe('WEIGHTED_AVERAGE')
      expect(INITIAL_FORM.scoring.passThreshold).toBe(7.0)
      expect(INITIAL_FORM.scoring.warnThreshold).toBe(4.0)
    })

    it('SCORING_METHOD_DESCRIPTIONS covers all methods', () => {
      expect(SCORING_METHOD_DESCRIPTIONS).toHaveProperty('WEIGHTED_AVERAGE')
      expect(SCORING_METHOD_DESCRIPTIONS).toHaveProperty('PASS_FAIL')
      expect(SCORING_METHOD_DESCRIPTIONS).toHaveProperty('WORST_CASE')
    })
  })
})
