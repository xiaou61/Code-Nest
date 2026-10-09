import { createApiClient } from '@paideia/core'

import { API_BASE_URL } from './env'

/**
 * 令牌读取的唯一接缝。
 *
 * <p>API 客户端与会话读取**共用这一个函数**，所以接入认证时只改这一处：
 * 两个消费者会同时拿到令牌，不存在"请求带上令牌了但界面还认为未登录"这种不一致。
 *
 * <p>当前返回 null —— 还没有登录流程，因此 `/api/v1/me` 会被后端拒为 401，
 * 管理端也会被守卫拦在门外。这是刻意的事实，不是缺陷。
 */
export function getToken(): string | null {
  return null
}

export const api = createApiClient({
  baseUrl: API_BASE_URL,
  getToken,
})
