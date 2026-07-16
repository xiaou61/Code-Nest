import request from '@/utils/request'

/**
 * 用户首页聚合 API。
 */
export const homeApi = {
  getOverview(config = {}) {
    return request.get('/user/home/overview', {}, config)
  }
}

export default homeApi
