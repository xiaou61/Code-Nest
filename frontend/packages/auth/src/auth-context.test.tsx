import { act, render } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { AuthApi, AuthTokens } from './auth-api'
import { AuthProvider, useAuth, type AuthContextValue } from './auth-context'
import { createTokenStore, type TokenStorage } from './token-store'

/**
 * 会话刷新的并发行为。
 *
 * <p>这里的缺口原本很难发现：进页面时"挂载恢复"与"首个 401 回调"会各发起一次刷新，
 * 而 refresh 是一次性的——两次并发必然一胜一败，败的那次会被后端判为令牌重用并吊销整条链，
 * 表现就是"刷新页面后随机被踢下线"。
 */
describe('会话刷新', () => {
  function memoryStorage(initial: Record<string, string> = {}): TokenStorage {
    const store = new Map(Object.entries(initial))
    return {
      get: (key) => store.get(key) ?? null,
      set: (key, value) => {
        store.set(key, value)
      },
      remove: (key) => {
        store.delete(key)
      },
    }
  }

  function issued(refreshToken: string): AuthTokens {
    return {
      accessToken: `access-${refreshToken}`,
      accessExpiresAt: '2026-10-10T00:00:00Z',
      refreshToken,
      refreshExpiresAt: '2026-10-24T00:00:00Z',
      user: { id: '1', username: 'learner', email: 'learner@example.com', role: 'learner' },
    }
  }

  it('挂载恢复与并发调用共用同一次刷新：refresh 只发一次', async () => {
    // 让刷新一直挂着，从而把并发窗口固定下来（真实场景里它由网络延迟撑开）
    let release!: () => void
    const gate = new Promise<void>((resolve) => {
      release = resolve
    })
    const refresh = vi.fn(async () => {
      await gate
      return issued('refresh-2')
    })
    const api = {
      captcha: vi.fn(),
      sendEmailCode: vi.fn(),
      register: vi.fn(),
      login: vi.fn(),
      logout: vi.fn(),
      refresh,
    } as unknown as AuthApi
    const tokens = createTokenStore(memoryStorage({ 'refresh-token': 'refresh-1' }))

    let session: AuthContextValue | null = null
    function Probe() {
      session = useAuth()
      return null
    }

    render(
      <AuthProvider tokens={tokens} api={api}>
        <Probe />
      </AuthProvider>,
    )

    // 挂载恢复已经发起了一次（还挂着），这两次调用必须搭上同一个 Promise
    await act(async () => {
      const first = session!.refreshSession()
      const second = session!.refreshSession()
      release()
      await Promise.all([first, second])
    })

    expect(refresh).toHaveBeenCalledTimes(1)
    expect(tokens.refresh()).toBe('refresh-2')
  })

  it('网络故障不结束会话（只有服务端明确拒绝才算）', async () => {
    const api = {
      captcha: vi.fn(),
      sendEmailCode: vi.fn(),
      register: vi.fn(),
      login: vi.fn(),
      logout: vi.fn(),
      refresh: vi.fn(async () => {
        throw new TypeError('Failed to fetch')
      }),
    } as unknown as AuthApi
    const tokens = createTokenStore(memoryStorage({ 'refresh-token': 'refresh-1' }))

    let session: AuthContextValue | null = null
    function Probe() {
      session = useAuth()
      return null
    }

    render(
      <AuthProvider tokens={tokens} api={api}>
        <Probe />
      </AuthProvider>,
    )

    await act(async () => {
      await expect(session!.refreshSession()).resolves.toBe(false)
    })

    // refresh 仍然有效，不能因为一次断网把用户登出
    expect(tokens.refresh()).toBe('refresh-1')
  })
})
