import request from '@/utils/request'

/**
 * 版本历史用户端API
 */

export const versionApi = {
  // 获取版本历史时间轴（分页查询）
  getVersionTimeline(params) {
    return request.post('/version/timeline', params)
  },

  // 获取版本详情
  getVersionDetail(id) {
    return request.get(`/version/${id}`)
  },

  // 记录版本查看次数
  recordViewCount(id) {
    return request.post('/version/view', id)
  },

  // 搜索版本历史
  searchVersions(params) {
    return request.post('/version/search', params)
  },

  // 获取最新版本列表（用于首页展示等）
  getLatestVersions(limit = 5, config = {}) {
    return request.get('/version/latest', { limit }, config)
  }
}
