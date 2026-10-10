import { describe, expect, it, vi } from 'vitest'

import { createAuthorizedFetch } from './authorized-fetch'
import { createTokenStore } from './token-store'

function memoryStorage() {
  const store = new Map<string, string>()
  return {
    store,
    storage: {
      get: (key: string) => store.get(key) ?? null,
      set: (key: string, value: string) => {
        store.set(key, value)
      },
      remove: (key: string) => {
        store.delete(key)
      },
    },
  }
}

function jsonResponse(status: number, body: unknown = {}): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('令牌存储', () => {
  it('access 只放内存，落盘的只有 refresh', () => {
    const { storage, store } = memoryStorage()
    const tokens = createTokenStore(storage)

    tokens.set({ accessToken: 'access-1', refreshToken: 'refresh-1' })

    expect(tokens.access()).toBe('access-1')
    expect([...store.keys()]).toEqual(['refresh-token'])
    expect(store.get('refresh-token')).toBe('refresh-1')
  })

  it('重新构造时能从存储里恢复 refresh，但拿不到 access', () => {
    const { storage } = memoryStorage()
    createTokenStore(storage).set({ accessToken: 'access-1', refreshToken: 'refresh-1' })

    const restored = createTokenStore(storage)

    expect(restored.refresh()).toBe('refresh-1')
    expect(restored.access()).toBeNull()
  })

  it('clear 会清掉内存与存储两处', () => {
    const { storage, store } = memoryStorage()
    const tokens = createTokenStore(storage)
    tokens.set({ accessToken: 'a', refreshToken: 'r' })

    tokens.clear()

    expect(tokens.access()).toBeNull()
    expect(tokens.refresh()).toBeNull()
    expect(store.has('refresh-token')).toBe(false)
  })
})

describe('带令牌的 fetch', () => {
  it('请求会带上 Authorization 头', async () => {
    const { storage } = memoryStorage()
    const tokens = createTokenStore(storage)
    tokens.set({ accessToken: 'access-1', refreshToken: 'refresh-1' })
    const baseFetch = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => jsonResponse(200))
    const fetchWithAuth = createAuthorizedFetch({
      tokens,
      refresh: async () => true,
      onSessionLost: () => {},
      baseFetch: baseFetch as unknown as typeof fetch,
    })

    await fetchWithAuth('/api/v1/me')

    const init = baseFetch.mock.calls[0]?.[1]
    expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer access-1')
  })

  it('并发多个 401 只触发一次刷新，然后各自重放', async () => {
    const { storage } = memoryStorage()
    const tokens = createTokenStore(storage)
    tokens.set({ accessToken: 'stale', refreshToken: 'refresh-1' })

    let calls = 0
    const baseFetch = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => {
      calls += 1
      // 前两次（并发的两个原请求）返回 401，重放时返回 200
      return calls <= 2 ? jsonResponse(401) : jsonResponse(200)
    })
    const refresh = vi.fn(async () => {
      // 刷新有延迟，模拟真实网络：这正是并发窗口的来源
      await new Promise((resolve) => setTimeout(resolve, 10))
      tokens.set({ accessToken: 'fresh', refreshToken: 'refresh-2' })
      return true
    })
    const fetchWithAuth = createAuthorizedFetch({
      tokens,
      refresh,
      onSessionLost: () => {},
      baseFetch: baseFetch as unknown as typeof fetch,
    })

    const [first, second] = await Promise.all([fetchWithAuth('/api/v1/a'), fetchWithAuth('/api/v1/b')])

    // 核心断言：refresh 是一次性的，必须只调用一次
    expect(refresh).toHaveBeenCalledTimes(1)
    expect(first.status).toBe(200)
    expect(second.status).toBe(200)
    // 重放时用的是新令牌
    const replayed = baseFetch.mock.calls[2]?.[1]
    expect(new Headers(replayed?.headers).get('Authorization')).toBe('Bearer fresh')
  })

  it('刷新失败时会话结束，只回调一次且不重放', async () => {
    const { storage } = memoryStorage()
    const tokens = createTokenStore(storage)
    tokens.set({ accessToken: 'stale', refreshToken: 'refresh-1' })
    const baseFetch = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => jsonResponse(401))
    const onSessionLost = vi.fn()
    const fetchWithAuth = createAuthorizedFetch({
      tokens,
      refresh: async () => false,
      onSessionLost,
      baseFetch: baseFetch as unknown as typeof fetch,
    })

    const response = await fetchWithAuth('/api/v1/me')

    expect(response.status).toBe(401)
    expect(onSessionLost).toHaveBeenCalledTimes(1)
    // 原请求 + 一次刷新判定，没有额外的重放
    expect(baseFetch).toHaveBeenCalledTimes(1)
  })

  it('认证端点自身的 401 不触发刷新（那是业务结果，不是令牌过期）', async () => {
    const { storage } = memoryStorage()
    const tokens = createTokenStore(storage)
    const baseFetch = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => jsonResponse(401))
    const refresh = vi.fn(async () => true)
    const fetchWithAuth = createAuthorizedFetch({
      tokens,
      refresh,
      onSessionLost: () => {},
      baseFetch: baseFetch as unknown as typeof fetch,
    })

    const response = await fetchWithAuth('/api/v1/auth/login', { method: 'POST' })

    expect(response.status).toBe(401)
    expect(refresh).not.toHaveBeenCalled()
  })
})
