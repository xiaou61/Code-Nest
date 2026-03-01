import request from '@/utils/request'

export const mockInterviewAdminApi = {
  // ==================== 方向配置 ====================
  getDirections() {
    return request.get('/admin/mock-interview/directions')
  },

  createDirection(data) {
    return request.post('/admin/mock-interview/directions', data)
  },

  updateDirection(id, data) {
    return request.put(`/admin/mock-interview/directions/${id}`, data)
  },

  deleteDirection(id) {
    return request.delete(`/admin/mock-interview/directions/${id}`)
  },

  updateDirectionStatus(id, status) {
    return request.put(`/admin/mock-interview/directions/${id}/status`, null, { params: { status } })
  },

  // ==================== 运营数据 ====================
  getStatsOverview(params = {}) {
    return request.get('/admin/mock-interview/stats/overview', params)
  },

  getSessions(data) {
    return request.post('/admin/mock-interview/sessions', data)
  },

  getSessionDetail(id) {
    return request.get(`/admin/mock-interview/sessions/${id}`)
  }
}

export default mockInterviewAdminApi

