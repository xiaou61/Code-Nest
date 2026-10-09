import type { KeyValueCache, Platform } from '@paideia/core'

/** 主进程提供的缓存操作。异步语义，因为要经 IPC。 */
export interface DesktopCacheBridge {
  get(key: string): Promise<string | null>
  set(key: string, value: string): Promise<void>
  remove(key: string): Promise<void>
}

/**
 * 桌面壳通过 preload 暴露给渲染进程的桥。
 *
 * <p>渲染进程不开 Node 集成（{@code contextIsolation} 打开、{@code nodeIntegration} 关闭），
 * 需要跨进程的能力一律经这个桥走 IPC。这个接口就是宿主与业务代码之间的唯一契约。
 */
export interface DesktopBridge {
  readonly cache: DesktopCacheBridge
  /**
   * 上次退出时缓存里已有的键值，由主进程在启动时读盘并注入。
   *
   * <p>**没有它，`cache.get` 在本次启动第一次写入之前永远是空的**：IPC 是异步的，
   * 而 `Platform.cache` 刻意是同步语义（见下），同步的 `get` 只能读一个进程内镜像，
   * 镜像没人填充就等于缓存不存在——表现是"用户的选择重启后丢失"。
   */
  readonly cacheSnapshot: Record<string, string>
  /** 渲染进程的服务地址，用于显示与排查。 */
  readonly rendererOrigin: string
}

declare global {
  interface Window {
    paideiaDesktop?: DesktopBridge
  }
}

/**
 * 由桥与启动快照构造同步语义的缓存。
 *
 * <p>拆成纯函数是为了能测：镜像的填充时机正是本包唯一出过真 bug 的地方。
 */
export function createBridgeCache(
  bridge: DesktopCacheBridge,
  snapshot: Record<string, string> = {},
): KeyValueCache {
  const mirror = new Map<string, string>()
  for (const [key, value] of Object.entries(snapshot)) {
    if (typeof value === 'string') {
      mirror.set(key, value)
    }
  }

  return {
    // 同步读走镜像，因此能在首帧（主题脚本执行时）拿到上次的选择
    get: (key) => mirror.get(key) ?? null,
    set: (key, value) => {
      mirror.set(key, value)
      void bridge.set(key, value)
    },
    remove: (key) => {
      mirror.delete(key)
      void bridge.remove(key)
    },
  }
}

function requireBridge(): DesktopBridge {
  const bridge = globalThis.window?.paideiaDesktop
  if (bridge === undefined) {
    throw new Error(
      '缺少桌面桥：这个包只能在 Electron 的渲染进程里使用，且 preload 必须已注入 window.paideiaDesktop',
    )
  }
  return bridge
}

export function createDesktopPlatform(): Platform {
  const bridge = requireBridge()
  return { kind: 'desktop', cache: createBridgeCache(bridge.cache, bridge.cacheSnapshot) }
}

export function desktopRendererOrigin(): string {
  return requireBridge().rendererOrigin
}
