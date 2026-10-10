import { createApiClient, type ApiClient } from '@paideia/core'
import { createAuthApi, createAuthorizedFetch, createTokenStore, type AuthApi, type TokenStore } from '@paideia/auth'
import { createWebPlatform } from '@paideia/platform-web'

import { API_BASE_URL } from './env'

/** 管理端只在浏览器里跑，因此不需要构建模式判断。 */
export const platform = createWebPlatform()

/**
 * 认证接线。与学习者端同构（见 apps/app/src/auth.ts 的说明），只少了桌面分支。
 *
 * <p>管理端**不引路由**：它只有一屏，登录视图是这一屏的一个状态而不是一条路由。
 */
export interface AuthWiring {
  tokens: TokenStore
  authApi: AuthApi
  api: ApiClient
  registerRefresh(impl: () => Promise<boolean>): void
}

const tokens = createTokenStore(platform.cache)
const authApi = createAuthApi(createApiClient({ baseUrl: API_BASE_URL }))

let refresh: () => Promise<boolean> = async () => false

const fetchWithAuth = createAuthorizedFetch({
  tokens,
  refresh: () => refresh(),
  onSessionLost: () => {
    // 会话失效由认证上下文切状态，界面随之回到登录视图
  },
})

const api = createApiClient({ baseUrl: API_BASE_URL, getToken: tokens.access, fetchImpl: fetchWithAuth })

export const auth: AuthWiring = {
  tokens,
  authApi,
  api,
  registerRefresh(impl) {
    refresh = impl
  },
}
