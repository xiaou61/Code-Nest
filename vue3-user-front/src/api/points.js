import request from '@/utils/request'

/**
 * 积分相关API
 */
export const pointsApi = {
  // 获取用户积分余额信息
  getPointsBalance(config = {}) {
    return request.get('/user/points/balance', {}, config)
  },

  // 用户每日打卡
  checkin() {
    return request.post('/user/points/checkin')
  },

  // 获取用户积分明细列表
  getPointsDetailList(data) {
    return request.post('/user/points/detail', data)
  },

  // 获取打卡日历
  getCheckinCalendar(yearMonth = null) {
    const data = yearMonth ? { yearMonth } : {}
    return request.post('/user/points/checkin-calendar', data)
  },

  // 获取打卡统计数据
  getCheckinStatistics(months = 3) {
    return request.post('/user/points/checkin-statistics', { months })
  }
}

export default pointsApi
