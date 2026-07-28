import request from '@/utils/request'

export const sreApi = {
  getIncidentSummary() {
    return request.get('/admin/sre/incidents/summary')
  },

  getIncidents(params) {
    return request.get('/admin/sre/incidents', params)
  },

  getInvestigationContext(id) {
    return request.get(`/admin/sre/incidents/${id}/investigation-context`)
  },

  acknowledgeIncident(id) {
    return request.post(`/admin/sre/incidents/${id}/ack`)
  },

  resolveIncident(id) {
    return request.post(`/admin/sre/incidents/${id}/resolve`)
  },

  generateRca(id) {
    return request.post(`/admin/sre/incidents/${id}/rca`, {}, { timeout: 180000 })
  },

  getRcaRuns(id, limit = 10) {
    return request.get(`/admin/sre/incidents/${id}/rca-runs`, { limit })
  },

  getRcaRun(id, runId) {
    return request.get(`/admin/sre/incidents/${id}/rca-runs/${runId}`)
  },

  saveRcaFeedback(id, runId, data) {
    return request.put(`/admin/sre/incidents/${id}/rca-runs/${runId}/feedback`, data)
  },

  getRcaEvaluationSample(id, runId) {
    return request.get(`/admin/sre/incidents/${id}/rca-runs/${runId}/evaluation-sample`)
  },

  promoteRcaEvaluationCase(id, runId, feedbackId) {
    return request.post(
      `/admin/sre/incidents/${id}/rca-runs/${runId}/evaluation-cases`,
      { feedbackId }
    )
  },

  getRcaEvaluationCases(limit = 50) {
    return request.get('/admin/sre/rca-evaluations/cases', { limit })
  },

  createRcaEvaluationSuite(data) {
    return request.post('/admin/sre/rca-evaluations/suites', data)
  },

  getRcaEvaluationSuites(limit = 20) {
    return request.get('/admin/sre/rca-evaluations/suites', { limit })
  },

  publishRcaEvaluationSuiteVersion(suiteId, data) {
    return request.post(`/admin/sre/rca-evaluations/suites/${suiteId}/versions`, data)
  },

  getRcaEvaluationSuiteVersions(suiteId, limit = 20) {
    return request.get(`/admin/sre/rca-evaluations/suites/${suiteId}/versions`, { limit })
  },

  getRcaEvaluationSuiteVersion(versionId) {
    return request.get(`/admin/sre/rca-evaluations/suite-versions/${versionId}`)
  },

  runRcaEvaluation(caseId, suiteVersionId) {
    return request.post(
      '/admin/sre/rca-evaluations/runs',
      caseId != null ? { caseId } : (suiteVersionId != null ? { suiteVersionId } : {}),
      { timeout: 1800000 }
    )
  },

  getRcaEvaluationRuns(limit = 20) {
    return request.get('/admin/sre/rca-evaluations/runs', { limit })
  },

  getRcaEvaluationRun(runId) {
    return request.get(`/admin/sre/rca-evaluations/runs/${runId}`)
  },

  getRcaEvaluationGate(runId) {
    return request.get(`/admin/sre/rca-evaluations/runs/${runId}/gate`)
  }
}

export default sreApi
