import request from '@/utils/request'

/**
 * 计划打卡相关API
 */
export const planApi = {
  // 创建计划
  createPlan(data) {
    return request.post('/user/plan/create', data)
  },

  // 更新计划
  updatePlan(planId, data) {
    return request.put(`/user/plan/update/${planId}`, data)
  },

  // 删除计划
  deletePlan(planId) {
    return request.delete(`/user/plan/${planId}`)
  },

  // 获取计划详情
  getPlanDetail(planId) {
    return request.get(`/user/plan/${planId}`)
  },

  // 获取计划列表
  getPlanList(data) {
    return request.post('/user/plan/list', data)
  },

  // 暂停计划
  pausePlan(planId) {
    return request.put(`/user/plan/${planId}/pause`)
  },

  // 恢复计划
  resumePlan(planId) {
    return request.put(`/user/plan/${planId}/resume`)
  },

  // 获取今日任务
  getTodayTasks() {
    return request.get('/user/plan/today-tasks')
  },

  // 执行打卡
  checkin(data) {
    return request.post('/user/plan/checkin', data)
  },

  // 获取打卡记录
  getCheckinRecords(planId) {
    return request.get(`/user/plan/${planId}/checkin/list`)
  },

  // 获取统计概览
  getStatsOverview(config = {}) {
    return request.get('/user/plan/stats/overview', {}, config)
  }
}

export default planApi
