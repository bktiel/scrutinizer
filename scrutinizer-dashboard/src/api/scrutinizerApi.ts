import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export interface PostureRunSummary {
  id: string
  applicationName: string
  policyName: string
  policyVersion: string
  overallDecision: string
  postureScore: number
  runTimestamp: string
}

export interface Finding {
  id: string
  ruleId: string
  decision: string
  severity: string
  field: string
  actualValue: string
  expectedValue: string
  description: string
  remediation: string
  componentRef: string
  componentName: string
}

export interface ComponentResult {
  id: string
  componentRef: string
  componentName: string
  componentVersion: string
  purl: string
  isDirect: boolean
  decision: string
  findings: Finding[]
}

export interface PostureRunDetail {
  id: string
  applicationName: string
  sbomHash: string
  policyName: string
  policyVersion: string
  overallDecision: string
  postureScore: number
  summaryJson: string
  runTimestamp: string
  createdAt: string
  componentResults: ComponentResult[]
}

export interface TrendDataPoint {
  timestamp: string
  postureScore: number
  overallDecision: string
  policyVersion: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export async function listRuns(page = 0, size = 20, applicationName?: string): Promise<Page<PostureRunSummary>> {
  const params: Record<string, string | number> = { page, size }
  if (applicationName) params.applicationName = applicationName
  const { data } = await api.get('/runs', { params })
  return data
}

export async function getRunDetail(id: string): Promise<PostureRunDetail> {
  const { data } = await api.get(`/runs/${id}`)
  return data
}

export async function getRunFindings(id: string, page = 0, size = 50, decision?: string): Promise<Page<Finding>> {
  const params: Record<string, string | number> = { page, size }
  if (decision) params.decision = decision
  const { data } = await api.get(`/runs/${id}/findings`, { params })
  return data
}

export async function getTrends(applicationName: string): Promise<TrendDataPoint[]> {
  const { data } = await api.get('/runs/trends', { params: { applicationName } })
  return data
}

export async function createRun(applicationName: string, sbomPath: string, policyPath: string): Promise<PostureRunSummary> {
  const { data } = await api.post('/runs', { applicationName, sbomPath, policyPath })
  return data
}
