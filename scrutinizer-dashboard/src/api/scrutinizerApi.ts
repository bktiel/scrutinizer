const BASE = '/api/v1'

/** Unwrap a response that may be a Spring Page object or a plain array */
function unwrapPage<T>(data: unknown): T[] {
  if (Array.isArray(data)) return data
  if (data && typeof data === 'object' && 'content' in data && Array.isArray((data as any).content)) {
    return (data as any).content
  }
  return []
}

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

// async function post<T>(path: string, body: unknown): Promise<T> {
//   const res = await fetch(`${BASE}${path}`, {
//     method: 'POST',
//     headers: { 'Content-Type': 'application/json' },
//     body: JSON.stringify(body),
//   })
//   if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
//   return res.json()
// }

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

export async function createPolicyFromYaml(yaml: string, description?: string): Promise<Policy> {
  const blob = new Blob([yaml], { type: 'application/x-yaml' })
  const file = new File([blob], 'policy.yaml', { type: 'application/x-yaml' })
  return uploadPolicy(file, description)
}

export async function updatePolicyFromYaml(id: string, yaml: string, description?: string): Promise<Policy> {
  const blob = new Blob([yaml], { type: 'application/x-yaml' })
  const file = new File([blob], 'policy.yaml', { type: 'application/x-yaml' })
  return updatePolicy(id, file, description)
}

// --- Projects ---

export interface Project {
  id: string
  name: string
  description: string | null
  repositoryUrl: string | null
  gitlabProjectId: string | null
  defaultBranch: string
  policyId: string | null
  policyName: string | null
  createdAt: string
  updatedAt: string
  stats: ProjectStats | null
}

export interface ProjectStats {
  totalRuns: number
  totalComponents: number
  passCount: number
  failCount: number
  warnCount: number
  latestScore: number
  latestDecision: string
  lastRunAt: string | null
  provenanceCoverage: number
  scorecardCoverage: number
}

export interface PolicyException {
  id: string
  projectId: string
  policyId: string | null
  ruleId: string | null
  packageName: string | null
  packageVersion: string | null
  justification: string
  createdBy: string
  approvedBy: string | null
  status: string
  scope: string
  expiresAt: string | null
  createdAt: string
  updatedAt: string
}

export async function listProjects(): Promise<Project[]> {
  const resp = await get<unknown>('/projects')
  return unwrapPage<Project>(resp)
}

export async function getProject(id: string): Promise<Project> {
  return get(`/projects/${id}`)
}

export async function createProject(data: { name: string; description?: string; repositoryUrl?: string; gitlabProjectId?: string; defaultBranch?: string; policyId?: string }): Promise<Project> {
  const res = await fetch(`${BASE}/projects`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

export async function updateProject(id: string, data: Record<string, unknown>): Promise<Project> {
  const res = await fetch(`${BASE}/projects/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

export async function deleteProject(id: string): Promise<void> {
  return del(`/projects/${id}`)
}

export async function assignProjectPolicy(id: string, policyId: string): Promise<Project> {
  const res = await fetch(`${BASE}/projects/${id}/policy`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ policyId }),
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

export async function getProjectRuns(id: string, page = 0, size = 20): Promise<Page<PostureRunSummary>> {
  return get(`/projects/${id}/runs`, { page, size })
}

export async function getProjectComponents(id: string): Promise<ComponentResult[]> {
  return get(`/projects/${id}/components`)
}

export async function getProjectTrends(id: string): Promise<TrendDataPoint[]> {
  return get(`/projects/${id}/trends`)
}

export async function getProjectExceptions(projectId: string): Promise<PolicyException[]> {
  const resp = await get<unknown>(`/projects/${projectId}/exceptions`)
  return unwrapPage<PolicyException>(resp)
}

export async function listExceptions(projectId?: string, status?: string): Promise<PolicyException[]> {
  const params: Record<string, string> = {}
  if (projectId) params.projectId = projectId
  if (status) params.status = status
  const resp = await get<unknown>('/exceptions', params)
  return unwrapPage<PolicyException>(resp)
}

export async function createException(data: { projectId: string; policyId?: string; ruleId?: string; packageName?: string; packageVersion?: string; justification: string; scope?: string; expiresAt?: string }): Promise<PolicyException> {
  const res = await fetch(`${BASE}/exceptions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

export async function updateException(id: string, data: Record<string, unknown>): Promise<PolicyException> {
  const res = await fetch(`${BASE}/exceptions/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

export async function deleteException(id: string): Promise<void> {
  return del(`/exceptions/${id}`)
}
