import request from '@/utils/request'

export const interviewApi = {
  // ============ 分类相关 ============
  
  // 获取启用的分类列表
  getEnabledCategories() {
    return request.get('/interview/categories')
  },
  
  // 获取分类详情
  getCategoryById(id) {
    return request.get(`/interview/categories/${id}`)
  },
  
  // ============ 题单相关 ============
  
  // 获取公开题单列表
  getPublicQuestionSets(params) {
    return request.get('/interview/question-sets', params)
  },
  
  // 获取题单详情
  getQuestionSetById(id) {
    return request.get(`/interview/question-sets/${id}`)
  },
  
  // 获取题单的题目列表
  getQuestionsBySetId(id) {
    return request.get(`/interview/question-sets/${id}/questions`)
  },
  
  // 获取题目详情
  getQuestionById(setId, questionId) {
    return request.get(`/interview/question-sets/${setId}/questions/${questionId}`)
  },
  
  // 获取下一题
  getNextQuestion(setId, questionId) {
    return request.get(`/interview/question-sets/${setId}/questions/${questionId}/next`)
  },
  
  // 获取上一题
  getPrevQuestion(setId, questionId) {
    return request.get(`/interview/question-sets/${setId}/questions/${questionId}/prev`)
  },
  
  // 搜索题目（标题和答案）
  searchQuestions(data) {
    return request.post('/interview/question-sets/search', data)
  },

  // 随机抽题
  getRandomQuestions(questionSetIds, count) {
    return request.post('/interview/question-sets/questions/random', {
      questionSetIds,
      count
    })
  },
  
  // ============ 收藏相关 ============
  
  // 添加收藏
  addFavorite(targetType, targetId) {
    return request.post('/interview/favorites/add', {
      targetType,
      targetId
    })
  },
  
  // 取消收藏
  removeFavorite(targetType, targetId) {
    return request.post('/interview/favorites/remove', {
      targetType,
      targetId
    })
  },
  
  // 检查是否已收藏
  isFavorited(targetType, targetId) {
    return request.post('/interview/favorites/check', {
      targetType,
      targetId
    })
  },
  
  // 获取我的收藏列表
  getMyFavorites(targetType) {
    return request.post('/interview/favorites/my', {
      targetType
    })
  },
  
  // 分页获取我的收藏列表
  getMyFavoritePage(targetType, page, size) {
    return request.post('/interview/favorites/my/page', {
      targetType,
      page,
      size
    })
  },
  
// 获取收藏统计
  getFavoriteCount(targetType, targetId) {
    return request.post('/interview/favorites/count', {
      targetType,
      targetId
    })
  },

  // ============ 学习记录相关 ============

  // 记录学习
  recordLearn(questionSetId, questionId) {
    return request.post('/interview/learn/record', {
      questionSetId,
      questionId
    })
  },

  // 获取题单学习进度
  getLearnProgress(questionSetId) {
    return request.get(`/interview/learn/progress/${questionSetId}`)
  },

  // 获取已学习题目ID列表
  getLearnedQuestions(questionSetId) {
    return request.get(`/interview/learn/questions/${questionSetId}`)
  },

  // 获取用户总学习数量
  getTotalLearned() {
    return request.get('/interview/learn/total')
  },

  // ============ 掌握度相关 ============

  // 标记题目掌握度
  markMastery(data) {
    return request.post('/interview/mastery/mark', data)
  },

  // 获取题目掌握度
  getMastery(questionId) {
    return request.get(`/interview/mastery/${questionId}`)
  },

  // 批量获取题目掌握度
  batchGetMastery(questionIds) {
    return request.post('/interview/mastery/batch', questionIds)
  },

  // ============ 复习相关 ============

  // 获取复习统计
  getReviewStats() {
    return request.get('/interview/mastery/review/stats')
  },

  // 获取待复习题目列表
  getReviewList(type = 'all') {
    return request.get('/interview/mastery/review/list', { type })
  },

  // ============ 热力图相关 ============

  // 获取学习热力图数据
  getHeatmap(year) {
    return request.get('/interview/mastery/heatmap', { year })
  },

  // 获取某日学习详情
  getHeatmapDetail(date) {
    return request.get('/interview/mastery/heatmap/detail', { date })
  }
}
