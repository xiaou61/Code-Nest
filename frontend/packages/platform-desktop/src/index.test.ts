import { describe, expect, it, vi } from 'vitest'

import { createBridgeCache } from './index'

/** 假桥：记录调用，异步返回，模拟真实 IPC 的时序。 */
function fakeBridge() {
  const calls: string[] = []
  return {
    calls,
    bridge: {
      get: vi.fn(async (key: string) => {
        calls.push(`get:${key}`)
        return null
      }),
      set: vi.fn(async (key: string, value: string) => {
        calls.push(`set:${key}=${value}`)
      }),
      remove: vi.fn(async (key: string) => {
        calls.push(`remove:${key}`)
      }),
    },
  }
}

describe('桌面端缓存', () => {
  it('启动快照里的值不经写入就能同步读到', () => {
    const { bridge, calls } = fakeBridge()
    const cache = createBridgeCache(bridge, { theme: 'dark' })

    // 这是本包出过的真 bug：镜像启动时不填充，这里会返回 null，
    // 于是主题偏好在桌面端每次启动都丢。
    expect(cache.get('theme')).toBe('dark')
    // 读走镜像，不该为一次读触发 IPC
    expect(calls).toEqual([])
  })

  it('写入后同步可见，并异步落盘', () => {
    const { bridge, calls } = fakeBridge()
    const cache = createBridgeCache(bridge, {})

    cache.set('theme', 'light')

    expect(cache.get('theme')).toBe('light')
    expect(bridge.set).toHaveBeenCalledWith('theme', 'light')
    expect(calls).toEqual(['set:theme=light'])
  })

  it('删除后同步不可见，并异步通知主进程', () => {
    const { bridge } = fakeBridge()
    const cache = createBridgeCache(bridge, { theme: 'dark' })

    cache.remove('theme')

    expect(cache.get('theme')).toBeNull()
    expect(bridge.remove).toHaveBeenCalledWith('theme')
  })

  it('快照缺失或值不是字符串时不抛错', () => {
    const { bridge } = fakeBridge()

    expect(createBridgeCache(bridge).get('theme')).toBeNull()

    const messy = { good: 'ok', number: 1, nothing: null } as unknown as Record<string, string>
    const cache = createBridgeCache(bridge, messy)

    expect(cache.get('good')).toBe('ok')
    expect(cache.get('number')).toBeNull()
    expect(cache.get('nothing')).toBeNull()
  })
})
