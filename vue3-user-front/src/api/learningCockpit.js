import request from '@/utils/request'

/**
 * 学习成长驾驶舱 API
 */
export const learningCockpitApi = {
  getOverview(params = {}) {
    return request.get('/user/learning-cockpit/overview', params)
  },

  saveTargetProfile(data = {}) {
    return request.post('/user/career-loop/profile', data)
  }
}

export default learningCockpitApi
