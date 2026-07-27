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
  }
}

export default sreApi
