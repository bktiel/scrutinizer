const BASE = '/api/v1'

async function get<T>(path: string, params?: Record<string, string | number>): Promise<T> {
  const url = new URL(path, window.location.origin)
  url.pathname = BASE + url.pathname
  if (params) {
    Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, String(v)))
  }
  const res = await fetch(url.toString())
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

async function del(path: string): Promise<void> {
  const res = await fetch(`${BASE}${path}`, { method: 'DELETE' })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
}

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

export interface Policy {
  id: string
  name: string
  version: string
  description: string | null
  policyYaml: string
  createdAt: string
  updatedAt: string
}

export interface PolicyHistory {
  id: string
  policyYaml: string
  changedBy: string | null
  changedAt: string
}

// --- Runs ---

export async function listRuns(page = 0, size = 20, applicationName?: string): Promise<Page<PostureRunSummary>> {
  const params: Record<string, string | number> = { page, size }
  if (applicationName) params.applicationName = applicationName
  return get('/runs', params)
}

export async function getRunDetail(id: string): Promise<PostureRunDetail> {
  return get(`/runs/${id}`)
}

export async function getRunFindings(id: string, page = 0, size = 50, decision?: string): Promise<Page<Finding>> {
  const params: Record<string, string | number> = { page, size }
  if (decision) params.decision = decision
  return get(`/runs/${id}/findings`, params)
}

export async function getTrends(applicationName: string): Promise<TrendDataPoint[]> {
  return get('/runs/trends', { applicationName })
}

export async function createRun(sbomFile: File, applicationName: string, policyId: string): Promise<PostureRunSummary> {
  const form = new FormData()
  form.append('sbom', sbomFile)
  form.append('applicationName', applicationName)
  form.append('policyId', policyId)
  const res = await fetch(`${BASE}/runs`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

// --- Policies ---

export async function listPolicies(): Promise<Policy[]> {
  return get('/policies')
}

export async function getPolicy(id: string): Promise<Policy> {
  return get(`/policies/${id}`)
}

export async function uploadPolicy(file: File, description?: string): Promise<Policy> {
  const form = new FormData()
  form.append('file', file)
  if (description) form.append('description', description)
  const res = await fetch(`${BASE}/policies`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

export async function updatePolicy(id: string, file: File, description?: string): Promise<Policy> {
  const form = new FormData()
  form.append('file', file)
  if (description) form.append('description', description)
  const res = await fetch(`${BASE}/policies/${id}`, { method: 'PUT', body: form })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

export async function deletePolicy(id: string): Promise<void> {
  return del(`/policies/${id}`)
}

export async function getPolicyHistory(id: string): Promise<PolicyHistory[]> {
  return get(`/policies/${id}/history`)
}
