import { createApiClient } from '@paideia/core'

import { API_BASE_URL } from './env'

/**
 * 全局 API 客户端。令牌读取暂时返回 null（认证在 TASK-005 接入）；
 * 调用点已经收在这里，接入认证时只改这一处，业务代码不动。
 */
export const api = createApiClient({
  baseUrl: API_BASE_URL,
  getToken: () => null,
})
