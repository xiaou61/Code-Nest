import request from '@/utils/request'

export const interviewApi = {
  // ============ 分类管理 ============

  // 创建分类
  createCategory(data) {
    return request.post('/admin/interview/categories', data)
  },

  // 更新分类
  updateCategory(id, data) {
    return request.put(`/admin/interview/categories/${id}`, data)
  },

  // 删除分类
  deleteCategory(id) {
    return request.delete(`/admin/interview/categories/${id}`)
  },

  // 获取分类详情
  getCategoryById(id) {
    return request.get(`/admin/interview/categories/${id}`)
  },

  // 获取所有分类
  getAllCategories() {
    return request.get('/admin/interview/categories')
  },

  // ============ 题单管理 ============

  // 创建题单
  createQuestionSet(data) {
    return request.post('/admin/interview/question-sets', data)
  },

  // 更新题单
  updateQuestionSet(id, data) {
    return request.put(`/admin/interview/question-sets/${id}`, data)
  },

  // 删除题单
  deleteQuestionSet(id) {
    return request.delete(`/admin/interview/question-sets/${id}`)
  },

  // 获取题单详情
  getQuestionSetById(id) {
    return request.get(`/admin/interview/question-sets/${id}`)
  },

  // 分页查询题单
  getQuestionSets(params) {
    return request.post('/admin/interview/question-sets/list', params)
  },

  // 获取用户的题单
  getUserQuestionSets(userId) {
    return request.get(`/admin/interview/question-sets/user/${userId}`)
  },

  // 导入Markdown题目
  importMarkdownQuestions(data) {
    return request.post('/admin/interview/question-sets/import', data)
  },

  // 增加浏览次数
  increaseQuestionSetViewCount(id) {
    return request.post(`/admin/interview/question-sets/${id}/view`)
  },

  // 搜索题目（标题和答案）
  searchQuestions(data) {
    return request.post('/admin/interview/questions/search', data)
  },

  // ============ 题目管理 ============

  // 创建题目
  createQuestion(data) {
    return request.post('/admin/interview/questions', data)
  },

  // 更新题目
  updateQuestion(id, data) {
    return request.put(`/admin/interview/questions/${id}`, data)
  },

  // 删除题目
  deleteQuestion(id) {
    return request.delete(`/admin/interview/questions/${id}`)
  },

  // 获取题目详情
  getQuestionById(id) {
    return request.get(`/admin/interview/questions/${id}`)
  },

  // 分页查询题目
  getQuestions(params) {
    return request.post('/admin/interview/questions/list', params)
  },

  // 获取题单的题目列表
  getQuestionsBySetId(questionSetId) {
    return request.get(`/admin/interview/questions/set/${questionSetId}`)
  },

  // 获取下一题
  getNextQuestion(questionSetId, currentSortOrder) {
    return request.get(`/admin/interview/questions/set/${questionSetId}/next`, {
      params: { currentSortOrder }
    })
  },

  // 获取上一题
  getPrevQuestion(questionSetId, currentSortOrder) {
    return request.get(`/admin/interview/questions/set/${questionSetId}/prev`, {
      params: { currentSortOrder }
    })
  },

  // 增加题目浏览次数
  increaseQuestionViewCount(id) {
    return request.post(`/admin/interview/questions/${id}/view`)
  },

  // 批量删除题目
  batchDeleteQuestions(ids) {
    return request.delete('/admin/interview/questions/batch', { data: ids })
  }
}
