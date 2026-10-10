import { ApiError } from '@paideia/core'
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'

import { createAuthApi, type AuthApi, type AuthTokens, type AuthUser } from './auth-api'
import type { TokenStore } from './token-store'

/**
 * 认证上下文：会话状态、登录/注册/登出、以及 401 刷新所用的 `refresh` 回调。
 *
 * <p>业务页面只从这里拿会话与动作，**不得直接读写令牌**——令牌读写只在本包内。
 */

export type AuthStatus = 'loading' | 'signed-in' | 'signed-out'

export interface AuthContextValue {
  status: AuthStatus
  user: AuthUser | null
  login(identifier: string, password: string): Promise<void>
  register(input: {
    username: string
    email: string
    password: string
    code: string
  }): Promise<void>
  logout(): Promise<void>
  /** 供 `createAuthorizedFetch` 使用：用 refresh 换新令牌，成功返回 true。 */
  refreshSession(): Promise<boolean>
  /** 供验证码等表单直接调后端用。 */
  api: AuthApi
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({
  tokens,
  api,
  children,
  onNeedsLogin,
}: {
  tokens: TokenStore
  /** 与 fetch 层共用同一个客户端，避免两套解析。 */
  api: AuthApi
  children: ReactNode
  /** 会话彻底失效（refresh 也无效）时通知应用，用于跳回登录页。 */
  onNeedsLogin?: () => void
}) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<AuthUser | null>(null)
  // 首次挂载时尝试用已保存的 refresh 恢复会话
  const started = useRef(false)

  const apply = useCallback(
    (issued: AuthTokens) => {
      tokens.set(issued)
      setUser(issued.user)
      setStatus('signed-in')
    },
    [tokens],
  )

  const clear = useCallback(() => {
    tokens.clear()
    setUser(null)
    setStatus('signed-out')
  }, [tokens])

  const refreshSession = useCallback(async () => {
    const refreshToken = tokens.refresh()
    if (refreshToken === null) {
      return false
    }
    try {
      apply(await api.refresh(refreshToken))
      return true
    } catch {
      // refresh 过期、被吊销、或检测到重用：会话结束，不再重试
      clear()
      return false
    }
  }, [api, apply, clear, tokens])

  useEffect(() => {
    if (started.current) {
      return
    }
    started.current = true
    if (tokens.refresh() === null) {
      setStatus('signed-out')
      return
    }
    void refreshSession()
  }, [refreshSession, tokens])

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      api,
      refreshSession,
      login: async (identifier, password) => {
        apply(await api.login(identifier, password))
      },
      register: async (input) => {
        apply(await api.register(input))
      },
      logout: async () => {
        const refreshToken = tokens.refresh()
        clear()
        if (refreshToken !== null) {
          try {
            await api.logout(refreshToken)
          } catch {
            // 登出失败不该把用户卡在已登录状态：本地已经清了，服务端那条 refresh 到期自然失效
          }
        }
      },
    }),
    [api, apply, clear, refreshSession, status, tokens, user],
  )

  // fetch 层刷新失败时回调这里，界面随之回到登录页
  useEffect(() => {
    if (status === 'signed-out' && onNeedsLogin !== undefined) {
      onNeedsLogin()
    }
  }, [onNeedsLogin, status])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (value === null) {
    throw new Error('缺少 AuthProvider：会话必须通过 useAuth() 获取')
  }
  return value
}

/** 把任意错误转成可展示的文案。后端给的是给用户看的中文，直接用。 */
export function messageOf(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }
  return error instanceof Error ? error.message : '请求失败，请稍后重试'
}
