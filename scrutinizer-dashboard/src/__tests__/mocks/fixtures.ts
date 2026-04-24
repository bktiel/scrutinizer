import type { PostureRunSummary, PostureRunDetail, Policy, Finding, TrendDataPoint, ComponentResult } from '../../api/scrutinizerApi'

export const samplePolicy: Policy = {
  id: 'pol-1',
  name: 'npm-baseline',
  version: '1.0',
  description: 'Baseline npm policy',
  policyYaml: 'apiVersion: scrutinizer/v1\nmetadata:\n  name: npm-baseline\n  version: "1.0"\nrules: []\nscoring:\n  method: WEIGHTED_AVERAGE\n  passThreshold: 7.0\n  warnThreshold: 4.0',
  createdAt: '2026-04-01T00:00:00Z',
  updatedAt: '2026-04-01T00:00:00Z',
}

export const samplePolicies: Policy[] = [
  samplePolicy,
  { ...samplePolicy, id: 'pol-2', name: 'strict-policy', version: '2.0' },
]

export const sampleRunSummary: PostureRunSummary = {
  id: 'run-1',
  applicationName: 'test-app',
  policyName: 'npm-baseline',
  policyVersion: '1.0',
  overallDecision: 'PASS',
  postureScore: 85.5,
  runTimestamp: '2026-04-10T12:00:00Z',
}

export const sampleRunSummaries: PostureRunSummary[] = [
  sampleRunSummary,
  { ...sampleRunSummary, id: 'run-2', postureScore: 72.0, overallDecision: 'WARN', runTimestamp: '2026-04-09T12:00:00Z' },
  { ...sampleRunSummary, id: 'run-3', postureScore: 45.0, overallDecision: 'FAIL', runTimestamp: '2026-04-08T12:00:00Z' },
]

export const sampleFinding: Finding = {
  id: 'f-1',
  ruleId: 'scorecard-min',
  decision: 'FAIL',
  severity: 'FAIL',
  field: 'scorecard.score',
  actualValue: '3.5',
  expectedValue: '5.0',
  description: 'Scorecard score below minimum',
  remediation: 'Improve scorecard score to at least 5.0',
  componentRef: 'lodash@4.17.21',
  componentName: 'lodash',
}

export const sampleFindings: Finding[] = [
  sampleFinding,
  { ...sampleFinding, id: 'f-2', ruleId: 'provenance-required', decision: 'PASS', severity: 'WARN', componentRef: 'express@4.18.2', componentName: 'express' },
]

export const sampleComponentResult: ComponentResult = {
  id: 'cr-1',
  componentRef: 'express@4.18.2',
  componentName: 'express',
  componentVersion: '4.18.2',
  purl: 'pkg:npm/express@4.18.2',
  isDirect: true,
  decision: 'PASS',
  findings: [sampleFindings[1]],
}

export const sampleRunDetail: PostureRunDetail = {
  id: 'run-1',
  applicationName: 'test-app',
  sbomHash: 'abc123def456',
  policyName: 'npm-baseline',
  policyVersion: '1.0',
  overallDecision: 'PASS',
  postureScore: 85.5,
  summaryJson: '{"pass": 8, "warn": 1, "fail": 1, "total": 10}',
  runTimestamp: '2026-04-10T12:00:00Z',
  createdAt: '2026-04-10T12:00:00Z',
  componentResults: [
    sampleComponentResult,
    { ...sampleComponentResult, id: 'cr-2', componentRef: 'lodash@4.17.21', componentName: 'lodash', componentVersion: '4.17.21', decision: 'FAIL', isDirect: true, findings: [sampleFinding] },
  ],
}

export const sampleTrendData: TrendDataPoint[] = [
  { timestamp: '2026-04-01T00:00:00Z', postureScore: 60.0, overallDecision: 'FAIL', policyVersion: '1.0' },
  { timestamp: '2026-04-05T00:00:00Z', postureScore: 75.0, overallDecision: 'WARN', policyVersion: '1.0' },
  { timestamp: '2026-04-10T00:00:00Z', postureScore: 85.5, overallDecision: 'PASS', policyVersion: '1.0' },
]

export const sampleProject = {
  id: 'proj-1',
  name: 'test-project',
  description: 'A test project',
  repositoryUrl: 'https://gitlab.com/test/project',
  gitlabProjectId: '12345',
  defaultBranch: 'main',
  policyId: 'pol-1',
  policyName: 'npm-baseline',
  createdAt: '2026-04-01T00:00:00Z',
  updatedAt: '2026-04-01T00:00:00Z',
  stats: {
    totalRuns: 3,
    totalComponents: 10,
    passCount: 7,
    failCount: 2,
    warnCount: 1,
    latestScore: 85.5,
    latestDecision: 'PASS',
    lastRunAt: '2026-04-10T12:00:00Z',
    provenanceCoverage: 0.6,
    scorecardCoverage: 0.8,
  },
}

export const sampleProjects = [
  sampleProject,
  { ...sampleProject, id: 'proj-2', name: 'other-project', stats: { ...sampleProject.stats, totalRuns: 0, latestScore: 0, latestDecision: null, lastRunAt: null } },
]

export const sampleException = {
  id: 'exc-1',
  projectId: 'proj-1',
  policyId: 'pol-1',
  ruleId: 'scorecard-min',
  packageName: 'lodash',
  packageVersion: '4.17.21',
  justification: 'Known legacy dependency, upgrading in Q2',
  createdBy: 'system',
  approvedBy: null,
  status: 'ACTIVE',
  scope: 'PROJECT',
  expiresAt: '2026-06-01T00:00:00Z',
  createdAt: '2026-04-01T00:00:00Z',
  updatedAt: '2026-04-01T00:00:00Z',
}

export const sampleExceptions = [
  sampleException,
  { ...sampleException, id: 'exc-2', ruleId: 'provenance-required', packageName: null, scope: 'GLOBAL', status: 'ACTIVE' },
]
