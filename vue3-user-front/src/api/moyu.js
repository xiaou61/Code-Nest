import request from '@/utils/request'

/**
 * 摸鱼工具 API
 */

/**
 * 获取热榜分类信息
 */
export const getHotTopicCategories = () => {
  return request.get('/moyu/hot-topic/categories')
}

/**
 * 获取指定平台热榜数据
 * @param {string} platform - 平台代码
 */
export const getHotTopicData = (platform) => {
  return request.get(`/moyu/hot-topic/data/${platform}`)
}

/**
 * 获取所有平台热榜数据
 */
export const getAllHotTopicData = () => {
  return request.get('/moyu/hot-topic/data/all')
}

/**
 * 手动刷新热榜数据
 */
export const refreshHotTopicData = () => {
  return request.post('/moyu/hot-topic/refresh')
}

/**
 * 时薪计算器 API
 */

/**
 * 获取时薪计算器数据
 */
export const getSalaryCalculatorData = () => {
  return request.get('/moyu/salary-calculator/data')
}

/**
 * 获取用户薪资配置
 */
export const getSalaryConfig = () => {
  return request.get('/moyu/salary-calculator/config')
}

/**
 * 保存或更新薪资配置
 * @param {Object} config - 薪资配置
 */
export const saveOrUpdateSalaryConfig = (config) => {
  return request.post('/moyu/salary-calculator/config', config)
}

/**
 * 删除薪资配置
 */
export const deleteSalaryConfig = () => {
  return request.delete('/moyu/salary-calculator/config')
}

/**
 * 工作时间操作（开始/结束工作）
 * @param {Object} action - 操作信息
 */
export const handleWorkTime = (action) => {
  return request.post('/moyu/salary-calculator/work-time', action)
}

/**
 * 开发者日历 API
 */

/**
 * 获取今日推荐
 */
export const getTodayRecommend = () => {
  return request.get('/moyu/developer-calendar/today')
}

/**
 * 获取指定月份的日历数据
 * @param {number} year - 年份
 * @param {number} month - 月份
 */
export const getMonthCalendar = (year, month) => {
  return request.get(`/moyu/developer-calendar/month/${year}/${month}`)
}

/**
 * 获取指定日期的事件列表
 * @param {string} date - 日期 (yyyy-MM-dd)
 */
export const getEventsByDate = (date) => {
  return request.get(`/moyu/developer-calendar/events/${date}`)
}

/**
 * 根据事件类型获取事件列表
 * @param {number} eventType - 事件类型
 */
export const getEventsByType = (eventType) => {
  return request.get(`/moyu/developer-calendar/events/type/${eventType}`)
}

/**
 * 获取重要事件列表
 */
export const getMajorEvents = () => {
  return request.get('/moyu/developer-calendar/events/major')
}

/**
 * 获取用户偏好设置
 */
export const getUserCalendarPreference = () => {
  return request.get('/moyu/developer-calendar/preference')
}

/**
 * 保存用户偏好设置
 * @param {Object} preference - 偏好设置
 */
export const saveUserCalendarPreference = (preference) => {
  return request.post('/moyu/developer-calendar/preference', preference)
}

/**
 * 每日内容 API
 */

/**
 * 获取今日内容推荐
 */
export const getTodayContent = () => {
  return request.get('/moyu/daily-content/today')
}

/**
 * 根据内容类型获取内容列表
 * @param {number} contentType - 内容类型
 */
export const getContentsByType = (contentType) => {
  return request.get(`/moyu/daily-content/type/${contentType}`)
}

/**
 * 获取内容详情
 * @param {number} id - 内容ID
 */
export const getContentDetail = (id) => {
  return request.get(`/moyu/daily-content/${id}`)
}

/**
 * 点赞内容
 * @param {number} id - 内容ID
 */
export const likeContent = (id) => {
  return request.post(`/moyu/daily-content/${id}/like`)
}

/**
 * 收藏内容
 * @param {number} id - 内容ID
 */
export const collectContent = (id) => {
  return request.post(`/moyu/daily-content/${id}/toggle-collection`)
}

/**
 * 获取用户收藏列表
 * @param {Object} params - 查询参数
 */
export const getUserCollections = (params = {}) => {
  return request.get('/moyu/daily-content/collections', { params })
}

/**
 * Bug 商店 API
 */

/**
 * 随机获取一个Bug
 */
export const getRandomBug = () => {
  return request.post('/moyu/bug-store/random')
}
