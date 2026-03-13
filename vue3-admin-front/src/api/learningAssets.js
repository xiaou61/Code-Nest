import request from '@/utils/request'

export const learningAssetAdminApi = {
  getCandidates(params = {}) {
    return request.post('/admin/learning-assets/candidates/list', params)
  },

  getCandidateDetail(candidateId) {
    return request.get(`/admin/learning-assets/candidates/${candidateId}`)
  },

  updateCandidate(candidateId, data) {
    return request.put(`/admin/learning-assets/candidates/${candidateId}`, data)
  },

  approve(candidateId, data) {
    return request.post(`/admin/learning-assets/candidates/${candidateId}/approve`, data)
  },

  merge(candidateId, data) {
    return request.post(`/admin/learning-assets/candidates/${candidateId}/merge`, data)
  },

  reject(candidateId, data) {
    return request.post(`/admin/learning-assets/candidates/${candidateId}/reject`, data)
  },

  getStatistics() {
    return request.get('/admin/learning-assets/statistics')
  }
}

export default learningAssetAdminApi
