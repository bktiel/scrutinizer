import { http, HttpResponse } from 'msw'
import {
  samplePolicies,
  samplePolicy,
  sampleRunSummaries,
  sampleRunDetail,
  sampleFindings,
  sampleTrendData,
  sampleProjects,
  sampleProject,
  sampleExceptions,
  sampleException,
  sampleComponentResult,
} from './fixtures'

const BASE = '/api/v1'

export const handlers = [
  // --- Runs ---
  http.get(`${BASE}/runs`, () => {
    return HttpResponse.json({
      content: sampleRunSummaries,
      totalElements: sampleRunSummaries.length,
      totalPages: 1,
      number: 0,
      size: 20,
    })
  }),

  http.get(`${BASE}/runs/:id`, ({ params }) => {
    if (params.id === 'not-found') {
      return new HttpResponse(null, { status: 404 })
    }
    return HttpResponse.json(sampleRunDetail)
  }),

  http.get(`${BASE}/runs/:id/findings`, () => {
    return HttpResponse.json({
      content: sampleFindings,
      totalElements: sampleFindings.length,
      totalPages: 1,
      number: 0,
      size: 50,
    })
  }),

  http.get(`${BASE}/runs/trends`, () => {
    return HttpResponse.json(sampleTrendData)
  }),

  http.post(`${BASE}/runs`, () => {
    return HttpResponse.json(sampleRunSummaries[0], { status: 201 })
  }),

  // --- Policies ---
  http.get(`${BASE}/policies`, () => {
    return HttpResponse.json(samplePolicies)
  }),

  http.get(`${BASE}/policies/:id`, ({ params }) => {
    if (params.id === 'not-found') {
      return new HttpResponse(null, { status: 404 })
    }
    return HttpResponse.json(samplePolicy)
  }),

  http.post(`${BASE}/policies`, () => {
    return HttpResponse.json(samplePolicy, { status: 201 })
  }),

  http.put(`${BASE}/policies/:id`, () => {
    return HttpResponse.json(samplePolicy)
  }),

  http.delete(`${BASE}/policies/:id`, () => {
    return new HttpResponse(null, { status: 204 })
  }),

  http.get(`${BASE}/policies/:id/history`, () => {
    return HttpResponse.json([
      { id: 'h-1', policyYaml: samplePolicy.policyYaml, changedBy: 'system', changedAt: '2026-04-01T00:00:00Z' },
    ])
  }),

  // --- Projects ---
  http.get(`${BASE}/projects`, () => {
    return HttpResponse.json({
      content: sampleProjects,
      totalElements: sampleProjects.length,
      totalPages: 1,
      number: 0,
      size: 20,
    })
  }),

  http.get(`${BASE}/projects/:id`, ({ params }) => {
    if (params.id === 'not-found') {
      return new HttpResponse(null, { status: 404 })
    }
    return HttpResponse.json(sampleProject)
  }),

  http.post(`${BASE}/projects`, async ({ request }) => {
    const body = (await request.json()) as any
    return HttpResponse.json({ ...sampleProject, name: body.name }, { status: 201 })
  }),

  http.put(`${BASE}/projects/:id`, () => {
    return HttpResponse.json(sampleProject)
  }),

  http.delete(`${BASE}/projects/:id`, () => {
    return new HttpResponse(null, { status: 204 })
  }),

  http.put(`${BASE}/projects/:id/policy`, () => {
    return HttpResponse.json(sampleProject)
  }),

  http.get(`${BASE}/projects/:id/runs`, () => {
    return HttpResponse.json({
      content: sampleRunSummaries,
      totalElements: sampleRunSummaries.length,
      totalPages: 1,
      number: 0,
      size: 20,
    })
  }),

  http.get(`${BASE}/projects/:id/components`, () => {
    return HttpResponse.json([sampleComponentResult])
  }),

  http.get(`${BASE}/projects/:id/trends`, () => {
    return HttpResponse.json(sampleTrendData)
  }),

  http.get(`${BASE}/projects/:id/exceptions`, () => {
    return HttpResponse.json(sampleExceptions)
  }),

  // --- Exceptions ---
  http.get(`${BASE}/exceptions`, () => {
    return HttpResponse.json({
      content: sampleExceptions,
      totalElements: sampleExceptions.length,
      totalPages: 1,
      number: 0,
      size: 20,
    })
  }),

  http.post(`${BASE}/exceptions`, () => {
    return HttpResponse.json(sampleException, { status: 201 })
  }),

  http.put(`${BASE}/exceptions/:id`, () => {
    return HttpResponse.json({ ...sampleException, status: 'REVOKED' })
  }),

  http.delete(`${BASE}/exceptions/:id`, () => {
    return new HttpResponse(null, { status: 204 })
  }),

  // --- Admin ---
  http.get(`${BASE}/admin/cache-stats`, () => {
    return HttpResponse.json({ scorecardCacheSize: 5, provenanceCacheSize: 10 })
  }),

  http.delete(`${BASE}/admin/cache`, () => {
    return HttpResponse.json({ status: 'cleared' })
  }),
]
