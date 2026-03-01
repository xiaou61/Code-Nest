import request from '@/utils/request'

/**
 * 求职闭环中台 API
 */
export const careerLoopApi = {
  // 启动/获取会话
  start(data = {}) {
    return request.post('/user/career-loop/start', data)
  },

  // 当前状态
  getCurrent() {
    return request.get('/user/career-loop/current')
  },

  // 时间线
  getTimeline() {
    return request.get('/user/career-loop/timeline')
  },

  // 动作列表
  getActions() {
    return request.get('/user/career-loop/actions')
  },

  // 完成动作
  completeAction(actionId) {
    return request.post(`/user/career-loop/actions/${actionId}/done`)
  },

  // 手动同步
  sync(data = {}) {
    return request.post('/user/career-loop/sync', data)
  },

  // 上报事件
  event(data = {}) {
    return request.post('/user/career-loop/event', data)
  }
}

export default careerLoopApi

