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
  },

  // ============ 排行榜 ============

  // 获取排行榜
  getRanking(type = 'all') {
    return request.get('/oj/ranking', { params: { type } })
  },

  // ============ 赛事 ============  

  // 分页查询赛事
  getContestList(data) {
    return request.post('/oj/contests/list', data)
  },

  // 获取赛事详情
  getContestDetail(id) {
    return request.get(`/oj/contests/${id}`)
  },

  // 报名赛事
  joinContest(id) {
    return request.post(`/oj/contests/${id}/join`)
  },

  // 获取赛事榜单
  getContestRanking(id) {
    return request.get(`/oj/contests/${id}/ranking`)
  },

  // ============ 每日一题 ============

  // 获取每日一题
  getDailyProblem() {
    return request.get('/oj/daily-problem')
  },

  // ============ 题目评论 ============

  // 获取题目评论列表
  getComments(problemId, data) {
    return request.post(`/oj/problems/${problemId}/comments`, data)
  },

  // 发表评论
  createComment(problemId, data) {
    return request.post(`/oj/problems/${problemId}/comments/create`, data)
  },

  // 回复评论
  replyComment(commentId, data) {
    return request.post(`/oj/comments/${commentId}/reply`, data)
  },

  // 点赞评论
  likeComment(commentId) {
    return request.post(`/oj/comments/${commentId}/like`)
  },

  // 取消点赞评论
  unlikeComment(commentId) {
    return request.delete(`/oj/comments/${commentId}/like`)
  },

  // 获取评论的回复列表
  getCommentReplies(commentId, data) {
    return request.post(`/oj/comments/${commentId}/replies`, data)
  }
}
