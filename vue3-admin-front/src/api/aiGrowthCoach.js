import request from '@/utils/request'

/**
 * AI成长教练管理端 API
 */
export const aiGrowthCoachAdminApi = {
  getStatistics() {
    return request.get('/admin/ai-growth-coach/statistics')
  },

  getFailures() {
    return request.get('/admin/ai-growth-coach/failures')
  },

  getConfigs() {
    return request.get('/admin/ai-growth-coach/config')
  },

  updateConfigs(data = []) {
    return request.put('/admin/ai-growth-coach/config', data)
  }
}

export default aiGrowthCoachAdminApi
