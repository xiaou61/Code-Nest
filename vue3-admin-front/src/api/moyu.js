import request from '@/utils/request'

// ============ 开发者日历相关API ============
export const developerCalendarApi = {
  // 获取日历事件列表（分页）
  getEventList(params) {
    return request.get('/admin/moyu/developer-calendar/events', params)
  },
  
  // 获取事件详情
  getEventById(id) {
    return request.get(`/admin/moyu/developer-calendar/events/${id}`)
  },
  
  // 创建事件
  createEvent(data) {
    return request.post('/admin/moyu/developer-calendar/events', data)
  },
  
  // 更新事件
  updateEvent(id, data) {
    return request.put(`/admin/moyu/developer-calendar/events/${id}`, data)
  },
  
  // 删除事件
  deleteEvent(id) {
    return request.delete(`/admin/moyu/developer-calendar/events/${id}`)
  },
  
  // 批量删除事件
  batchDeleteEvents(ids) {
    return request.post('/admin/moyu/developer-calendar/events/batch-delete', ids)
  },
  
  // 获取事件统计
  getEventStatistics() {
    return request.get('/admin/moyu/developer-calendar/events/statistics')
  },
  
  // 用户端获取今日事件
  getTodayEvents() {
    return request.get('/moyu/developer-calendar/today')
  },
  
  // 更新事件状态
  updateEventStatus(id, status) {
    return request.post(`/admin/moyu/developer-calendar/events/${id}/status`, null, { status })
  }
}

// ============ 每日内容相关API ============
export const dailyContentApi = {
  // 获取内容列表（分页）
  getContentList(params) {
    return request.get('/admin/moyu/daily-content/list', params)
  },
  
  // 获取内容详情
  getContentById(id) {
    return request.get(`/admin/moyu/daily-content/${id}`)
  },
  
  // 创建内容
  createContent(data) {
    return request.post('/admin/moyu/daily-content', data)
  },
  
  // 更新内容
  updateContent(id, data) {
    return request.put(`/admin/moyu/daily-content/${id}`, data)
  },
  
  // 删除内容
  deleteContent(id) {
    return request.delete(`/admin/moyu/daily-content/${id}`)
  },
  
  // 批量删除内容
  batchDeleteContents(ids) {
    return request.post('/admin/moyu/daily-content/batch-delete', ids)
  },
  
  // 获取内容统计
  getContentStatistics() {
    return request.get('/admin/moyu/daily-content/statistics')
  },
  
  // 获取热门内容排行
  getPopularRanking(params) {
    return request.get('/admin/moyu/daily-content/popular-ranking', params)
  },
  
  // 获取收藏统计
  getCollectionStatistics() {
    return request.get('/admin/moyu/daily-content/collections/statistics')
  },
  
  // 更新内容状态
  updateContentStatus(id, status) {
    return request.post(`/admin/moyu/daily-content/${id}/status`, null, { status })
  },
  
  // 用户端获取每日内容
  getUserDailyContent(params) {
    return request.get('/moyu/daily-content/recommend', params)
  },
  
  // 用户端获取今日推荐
  getTodayRecommendation() {
    return request.get('/moyu/daily-content/today')
  },
  
  // 用户端点赞内容
  likeContent(id) {
    return request.post(`/moyu/daily-content/${id}/like`)
  },
  
  // 用户端收藏内容
  collectContent(id) {
    return request.post(`/moyu/daily-content/${id}/toggle-collection`)
  },
  
  // 用户端获取收藏列表
  getUserCollections(params) {
    return request.get('/moyu/daily-content/collections', params)
  }
}

// ============ 统计分析相关API ============
export const statisticsApi = {
  // 获取事件统计
  getEventStatistics() {
    return request.get('/admin/moyu/developer-calendar/events/statistics')
  },
  
  // 获取内容统计
  getContentStatistics() {
    return request.get('/admin/moyu/daily-content/statistics')
  },
  
  // 获取收藏统计
  getCollectionStatistics() {
    return request.get('/admin/moyu/daily-content/collections/statistics')
  },
  
  // 获取热门内容排行
  getPopularContent(params = { limit: 10 }) {
    return request.get('/admin/moyu/daily-content/popular-ranking', params)
  },
  
}

// ============ Bug商店相关API ============
export const bugStoreApi = {
  // 分页查询Bug列表
  getBugList(data) {
    return request.post('/admin/moyu/bug-store/list', data)
  },
  
  // 获取Bug详情
  getBugById(id) {
    return request.get(`/admin/moyu/bug-store/${id}`)
  },
  
  // 添加Bug
  addBug(data) {
    return request.post('/admin/moyu/bug-store', data)
  },
  
  // 更新Bug
  updateBug(id, data) {
    return request.put(`/admin/moyu/bug-store/${id}`, data)
  },
  
  // 删除Bug
  deleteBug(id) {
    return request.delete(`/admin/moyu/bug-store/${id}`)
  },
  
  // 批量导入Bug
  batchImportBugs(data) {
    return request.post('/admin/moyu/bug-store/batch-import', data)
  }
}

// ============ 导出所有API ============
export const moyuApi = {
  calendar: developerCalendarApi,
  content: dailyContentApi,
  statistics: statisticsApi,
  bugStore: bugStoreApi
}

export default moyuApi
