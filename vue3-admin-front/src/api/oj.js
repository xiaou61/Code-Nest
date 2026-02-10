import request from '@/utils/request'

export const ojApi = {
  // ============ 题目管理 ============

  // 创建题目
  createProblem(data, tagIds = []) {
    const params = tagIds.length > 0 ? { tagIds: tagIds.join(',') } : {}
    return request.post('/admin/oj/problems', data, { params })
  },

  // 更新题目
  updateProblem(id, data, tagIds = []) {
    const params = tagIds.length > 0 ? { tagIds: tagIds.join(',') } : {}
    return request.put(`/admin/oj/problems/${id}`, data, { params })
  },

  // 删除题目
  deleteProblem(id) {
    return request.delete(`/admin/oj/problems/${id}`)
  },

  // 获取题目详情
  getProblemDetail(id) {
    return request.get(`/admin/oj/problems/${id}`)
  },

  // 分页查询题目
  getProblemList(data) {
    return request.post('/admin/oj/problems/list', data)
  },

  // ============ 标签管理 ============

  // 获取所有标签
  getTags() {
    return request.get('/admin/oj/problems/tags')
  },

  // 创建标签
  createTag(data) {
    return request.post('/admin/oj/problems/tags', data)
  },

  // ============ 测试用例管理 ============

  // 添加测试用例
  addTestCase(data) {
    return request.post('/admin/oj/test-cases', data)
  },

  // 更新测试用例
  updateTestCase(id, data) {
    return request.put(`/admin/oj/test-cases/${id}`, data)
  },

  // 删除测试用例
  deleteTestCase(id) {
    return request.delete(`/admin/oj/test-cases/${id}`)
  },

  // 获取题目的测试用例
  getTestCasesByProblem(problemId) {
    return request.get(`/admin/oj/test-cases/problem/${problemId}`)
  },

  // ============ 标准答案管理 ============

  // 添加标准答案
  addSolution(data) {
    return request.post('/admin/oj/solutions', data)
  },

  // 更新标准答案
  updateSolution(id, data) {
    return request.put(`/admin/oj/solutions/${id}`, data)
  },

  // 删除标准答案
  deleteSolution(id) {
    return request.delete(`/admin/oj/solutions/${id}`)
  },

  // 获取题目的标准答案
  getSolutionsByProblem(problemId) {
    return request.get(`/admin/oj/solutions/problem/${problemId}`)
  }
}
