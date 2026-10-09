import type { KeyValueCache, Platform } from '@paideia/core'

/**
 * 桌面壳通过 preload 暴露给渲染进程的桥。
 *
 * <p>渲染进程不开 Node 集成（{@code contextIsolation} 打开、{@code nodeIntegration} 关闭），
 * 需要跨进程的能力一律经这个桥走 IPC。这个接口就是宿主与业务代码之间的唯一契约。
 */
export interface DesktopBridge {
  readonly cache: {
    get(key: string): Promise<string | null>
    set(key: string, value: string): Promise<void>
    remove(key: string): Promise<void>
  }
  /** 渲染进程的服务地址，用于显示与排查。 */
  readonly rendererOrigin: string
}

declare global {
  interface Window {
    paideiaDesktop?: DesktopBridge
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
  const cache: KeyValueCache = {
    // 缓存读写是异步的，但 Platform 端口刻意保持同步语义：读取走本地镜像，
    // 写入异步落盘。桌面端缓存只用于加速，读到旧值不影响正确性。
    get: (key) => cacheMirror.get(key) ?? null,
    set: (key, value) => {
      cacheMirror.set(key, value)
      void bridge.cache.set(key, value)
    },
    remove: (key) => {
      cacheMirror.delete(key)
      void bridge.cache.remove(key)
    },
  }
  return { kind: 'desktop', cache }
}

const cacheMirror = new Map<string, string>()

export function desktopRendererOrigin(): string {
  return requireBridge().rendererOrigin
}
