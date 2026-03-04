import request from '@/utils/request'

export const jobBattleApi = {
  // JD解析
  parseJd(data) {
    return request.post('/user/job-battle/jd/parse', data)
  },

  // 简历匹配评估
  matchResume(data) {
    return request.post('/user/job-battle/resume/match', data)
  },

  // 生成补短板计划
  generatePlan(data) {
    return request.post('/user/job-battle/plan/generate', data)
  },

  // 获取计划历史（分页）
  getPlanHistory(data) {
    return request.post('/user/job-battle/plan/history', data)
  },

  // 获取计划历史详情
  getPlanHistoryDetail(id) {
    return request.get(`/user/job-battle/plan/history/${id}`)
  },

  // 岗位匹配引擎：批量分析
  runMatchEngine(data) {
    return request.post('/user/job-battle/match-engine/run', data)
  },

  // 岗位匹配引擎：历史记录
  getMatchEngineHistory(data) {
    return request.post('/user/job-battle/match-engine/history', data)
  },

  // 岗位匹配引擎：历史详情
  getMatchEngineHistoryDetail(id) {
    return request.get(`/user/job-battle/match-engine/history/${id}`)
  },

  // 岗位匹配引擎：最近一次结果
  getLatestMatchEngineResult() {
    return request.get('/user/job-battle/match-engine/latest')
  },

  // 面试复盘总结
  reviewInterview(data) {
    return request.post('/user/job-battle/interview/review', data)
  }
}
