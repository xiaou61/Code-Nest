import request from '@/utils/request'

/**
 * 成长闭环自动驾驶 API
 */
export const growthAutopilotApi = {
  getDashboard(params = {}) {
    return request.get('/user/plan/autopilot/dashboard', params)
  },

  generate(data = {}) {
    return request.post('/user/plan/autopilot/generate', data)
  },

  replan(data = {}) {
    return request.post('/user/plan/autopilot/replan', data)
  },

  completeTask(taskId) {
    return request.post(`/user/plan/autopilot/tasks/${taskId}/complete`)
  },

  completeToday(weekStart) {
    return request.post('/user/plan/autopilot/tasks/today/complete', null, {
      params: weekStart ? { weekStart } : {}
    })
  },

  postponeTask(taskId) {
    return request.post(`/user/plan/autopilot/tasks/${taskId}/postpone`)
  }
}

export default growthAutopilotApi
