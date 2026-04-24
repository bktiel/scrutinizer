import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { server } from '../mocks/server'
import { http, HttpResponse } from 'msw'
import {
  listRuns,
  getRunDetail,
  getRunFindings,
  getTrends,
  listPolicies,
  getPolicy,
  deletePolicy,
  getPolicyHistory,
  listProjects,
  getProject,
  createProject,
  deleteProject,
  assignProjectPolicy,
  getProjectRuns,
  getProjectComponents,
  getProjectTrends,
  listExceptions,
  createException,
  deleteException,
} from '../../api/scrutinizerApi'

// MSW server lifecycle managed by vitest.setup.ts

describe('scrutinizerApi', () => {
  describe('Runs API', () => {
    it('listRuns returns paginated runs', async () => {
      const result = await listRuns()
      expect(result.content).toHaveLength(3)
      expect(result.content[0].id).toBe('run-1')
    })

    it('listRuns with applicationName filter', async () => {
      const result = await listRuns(0, 20, 'test-app')
      expect(result.content).toBeDefined()
    })

    it('getRunDetail returns full detail', async () => {
      const detail = await getRunDetail('run-1')
      expect(detail.id).toBe('run-1')
      expect(detail.applicationName).toBe('test-app')
      expect(detail.componentResults).toHaveLength(2)
    })

    it('getRunDetail throws on not found', async () => {
      await expect(getRunDetail('not-found')).rejects.toThrow('404')
    })

    it('getRunFindings returns paginated findings', async () => {
      const result = await getRunFindings('run-1')
      expect(result.content).toHaveLength(2)
    })

    it('getTrends returns trend data', async () => {
      const trends = await getTrends('test-app')
      expect(trends).toHaveLength(3)
      expect(trends[0].postureScore).toBe(60.0)
      expect(trends[2].postureScore).toBe(85.5)
    })
  })

  describe('Policies API', () => {
    it('listPolicies returns all policies', async () => {
      const policies = await listPolicies()
      expect(policies).toHaveLength(2)
      expect(policies[0].name).toBe('npm-baseline')
    })

    it('getPolicy returns single policy', async () => {
      const policy = await getPolicy('pol-1')
      expect(policy.name).toBe('npm-baseline')
      expect(policy.policyYaml).toContain('scrutinizer/v1')
    })

    it('getPolicy throws on not found', async () => {
      await expect(getPolicy('not-found')).rejects.toThrow('404')
    })

    it('deletePolicy succeeds', async () => {
      await expect(deletePolicy('pol-1')).resolves.toBeUndefined()
    })

    it('getPolicyHistory returns history entries', async () => {
      const history = await getPolicyHistory('pol-1')
      expect(history).toHaveLength(1)
      expect(history[0].changedBy).toBe('system')
    })
  })

  describe('Projects API', () => {
    it('listProjects returns unwrapped project list', async () => {
      const projects = await listProjects()
      expect(projects).toHaveLength(2)
      expect(projects[0].name).toBe('test-project')
    })

    it('getProject returns project with stats', async () => {
      const project = await getProject('proj-1')
      expect(project.name).toBe('test-project')
      expect(project.stats?.totalRuns).toBe(3)
      expect(project.stats?.latestScore).toBe(85.5)
    })

    it('createProject sends POST and returns result', async () => {
      const project = await createProject({ name: 'new-project' })
      expect(project.name).toBe('new-project')
    })

    it('deleteProject succeeds', async () => {
      await expect(deleteProject('proj-1')).resolves.toBeUndefined()
    })

    it('assignProjectPolicy sends PUT', async () => {
      const result = await assignProjectPolicy('proj-1', 'pol-1')
      expect(result.policyId).toBe('pol-1')
    })

    it('getProjectRuns returns paginated runs', async () => {
      const result = await getProjectRuns('proj-1')
      expect(result.content).toHaveLength(3)
    })

    it('getProjectComponents returns component list', async () => {
      const components = await getProjectComponents('proj-1')
      expect(components).toHaveLength(1)
      expect(components[0].componentName).toBe('express')
    })

    it('getProjectTrends returns trend data', async () => {
      const trends = await getProjectTrends('proj-1')
      expect(trends).toHaveLength(3)
    })
  })

  describe('Exceptions API', () => {
    it('listExceptions returns unwrapped list', async () => {
      const exceptions = await listExceptions()
      expect(exceptions).toHaveLength(2)
      expect(exceptions[0].ruleId).toBe('scorecard-min')
    })

    it('listExceptions with filters', async () => {
      const exceptions = await listExceptions('proj-1', 'ACTIVE')
      expect(exceptions).toBeDefined()
    })

    it('createException sends POST', async () => {
      const exc = await createException({
        projectId: 'proj-1',
        ruleId: 'scorecard-min',
        justification: 'Test exception',
      })
      expect(exc.id).toBe('exc-1')
      expect(exc.justification).toContain('legacy')
    })

    it('deleteException succeeds', async () => {
      await expect(deleteException('exc-1')).resolves.toBeUndefined()
    })
  })

  describe('Error handling', () => {
    it('throws on 500 server error', async () => {
      server.use(
        http.get('/api/v1/runs', () => {
          return new HttpResponse(null, { status: 500, statusText: 'Internal Server Error' })
        })
      )
      await expect(listRuns()).rejects.toThrow('500')
    })

    it('throws on network error', async () => {
      server.use(
        http.get('/api/v1/policies', () => {
          return HttpResponse.error()
        })
      )
      await expect(listPolicies()).rejects.toThrow()
    })
  })
})
