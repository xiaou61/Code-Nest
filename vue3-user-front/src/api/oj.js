import request from '@/utils/request'

export const ojApi = {
  // ============ 题目相关 ============

  // 分页查询公开题目
  getProblems(data) {
    return request.post('/oj/problems/list', data)
  },

  // 获取题目详情
  getProblemDetail(id) {
    return request.get(`/oj/problems/${id}`)
  },

  // 获取所有标签
  getTags() {
    return request.get('/oj/tags')
  },

  // ============ 提交相关 ============

  // 提交代码
  submitCode(data) {
    return request.post('/oj/submit', data)
  },

  // 查看提交详情
  getSubmissionDetail(id) {
    return request.get(`/oj/submissions/${id}`)
  },

  // 我的提交记录
  getMySubmissions(data) {
    return request.post('/oj/submissions/my', data)
  },

  // 某题的提交记录
  getProblemSubmissions(problemId, params = {}) {
    return request.get(`/oj/problems/${problemId}/submissions`, { params })
  },

  // ============ 统计相关 ============

  // 个人做题统计
  getMyStatistics() {
    return request.get('/oj/statistics/me')
  },

  // ============ 题解 ============

  // 获取题目的标准答案
  getSolutions(problemId) {
    return request.get(`/oj/problems/${problemId}/solutions`)
  },

  // ============ 练习场 ============

  // 自由运行代码
  runCode(data) {
    return request.post('/oj/run', data)
  },

  // 自测模式：编译+跑示例用例
  selfTest(data) {
    return request.post('/oj/test', data)
  }
}
