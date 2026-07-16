import request from '@/utils/request'

export const mockInterviewApi = {
  // ============ 配置相关 ============
  
  // 获取面试方向列表
  getDirections() {
    return request.get('/user/mock-interview/directions')
  },
  
  // 获取面试配置选项（难度、类型、风格、题目数量、出题模式）
  getConfig() {
    return request.get('/user/mock-interview/config')
  },
  
  // ============ 面试会话 ============
  
  // 创建面试（返回会话信息）
  createInterview(data) {
    return request.post('/user/mock-interview/create', data)
  },
  
  // 开始面试（获取第一题）- POST + body { sessionId }
  startInterview(sessionId) {
    return request.post('/user/mock-interview/session/start', { sessionId })
  },
  
  // 提交答案
  submitAnswer(data) {
    return request.post('/user/mock-interview/session/answer', data)
  },
  
  // 跳过当前题目 - POST + body { sessionId, qaId }
  skipQuestion(sessionId, qaId) {
    return request.post('/user/mock-interview/session/skip', { sessionId, qaId })
  },
  
  // 获取下一题 - GET + ?sessionId=
  getNextQuestion(sessionId) {
    return request.get('/user/mock-interview/session/next', { sessionId })
  },
  
  // 结束面试 - POST + body { sessionId }
  endInterview(sessionId) {
    return request.post('/user/mock-interview/session/end', { sessionId })
  },
  
  // 获取会话状态 - GET /session/{id}/status
  getSessionStatus(sessionId) {
    return request.get(`/user/mock-interview/session/${sessionId}/status`)
  },
  
  // ============ 历史记录 ============
  
  // 获取面试历史（分页）- POST + body
  getHistory(params) {
    return request.post('/user/mock-interview/history', params)
  },
  
  // 获取面试报告 - GET /{id}/report
  getReport(sessionId) {
    return request.get(`/user/mock-interview/${sessionId}/report`)
  },
  
  // 删除面试记录 - DELETE /{id}
  deleteInterview(sessionId) {
    return request.delete(`/user/mock-interview/${sessionId}`)
  },
  
  // ============ 统计 ============
  
  // 获取用户面试统计 - GET /stats/overview
  getStats(config = {}) {
    return request.get('/user/mock-interview/stats/overview', {}, config)
  },

  // ============ AI总结 ============
  
  // 生成面试总结 - POST /{id}/summary
  generateSummary(sessionId) {
    return request.post(`/user/mock-interview/${sessionId}/summary`)
  },

  // ============ 题库 ============
  
  // 获取可用题库列表 - GET /question-sets
  getQuestionSets(direction) {
    return request.get('/user/mock-interview/question-sets', { direction })
  },

  // ============ 追问 ============
  
  // 请求追问 - POST /session/follow-up
  requestFollowUp(sessionId, qaId) {
    return request.post('/user/mock-interview/session/follow-up', { sessionId, qaId })
  }
}
