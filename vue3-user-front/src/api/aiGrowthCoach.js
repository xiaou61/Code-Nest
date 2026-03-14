import request from '@/utils/request'

/**
 * AI成长教练 API
 */
export const aiGrowthCoachApi = {
  getSummary(scene = 'hybrid') {
    return request.get('/user/ai-growth-coach/summary', { scene })
  },

  getSnapshotDetail(snapshotId) {
    return request.get(`/user/ai-growth-coach/snapshots/${snapshotId}`)
  },

  refresh(data = {}) {
    return request.post('/user/ai-growth-coach/refresh', data)
  },

  chat(data = {}) {
    return request.post('/user/ai-growth-coach/chat', data)
  },

  replan(data = {}) {
    return request.post('/user/ai-growth-coach/replan', data)
  },

  getSession(sessionId) {
    return request.get(`/user/ai-growth-coach/chat/sessions/${sessionId}`)
  },

  updateActionStatus(actionId, status) {
    return request.post(`/user/ai-growth-coach/actions/${actionId}/status`, { status })
  }
}

export default aiGrowthCoachApi
