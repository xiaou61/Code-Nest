import { createApiClient, type ApiClient } from '@paideia/core'
import {
  createAuthApi,
  createAuthorizedFetch,
  createTokenStore,
  type AuthApi,
  type TokenStore,
} from '@paideia/auth'

import { API_BASE_URL } from './env'
import { platform } from './host'

/**
 * 认证接线：令牌、认证端点、带令牌的数据客户端，三者共用一份状态。
 *
 * <p><b>为什么这里有一个可替换的 refresh</b>：带令牌的 fetch 在遇到 401 时要刷新，
 * 而"怎么刷新、刷新之后界面怎么变"属于认证上下文（它才知道会话状态）。
 * 客户端又必须是页面可以直接 import 的稳定单例（页面不该自己去 new 一个）。
 * 两者相撞的解法是让上下文在挂载时把自己的刷新实现注册进来——见 `registerRefresh`。
 * 刷新失败时的界面变化由上下文自己处理（它会把状态置为已登出，登录门随之跳转），
 * 所以这里不需要额外的回调。
 */
export interface AuthWiring {
  tokens: TokenStore
  authApi: AuthApi
  api: ApiClient
  registerRefresh(impl: () => Promise<boolean>): void
}

export function createAuthWiring(): AuthWiring {
  const tokens = createTokenStore(platform.cache)

  // 认证端点自身不需要令牌，也不参与"401 后刷新重放"
  const authApi = createAuthApi(createApiClient({ baseUrl: API_BASE_URL }))

  let refresh: () => Promise<boolean> = async () => false

  const fetchWithAuth = createAuthorizedFetch({
    tokens,
    refresh: () => refresh(),
    onSessionLost: () => {
      // 会话失效时上下文已经把状态切成未登录，登录门会负责跳转；这里无需额外动作。
      // 保留这个钩子是为了让"谁负责跳转"这件事在代码里是明写的，而不是隐含依赖。
    },
  })

  const api = createApiClient({
    baseUrl: API_BASE_URL,
    getToken: tokens.access,
    fetchImpl: fetchWithAuth,
  })

  return {
    tokens,
    authApi,
    api,
    registerRefresh(impl) {
      refresh = impl
    },
  }
}

/** 应用级单例：页面直接 import 它，不必自己组装。 */
export const auth = createAuthWiring()
