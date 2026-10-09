import type { KeyValueCache, Platform } from '@paideia/core'

const KEY_PREFIX = 'paideia:'

function memoryCache(): KeyValueCache {
  const store = new Map<string, string>()
  return {
    get: (key) => store.get(KEY_PREFIX + key) ?? null,
    set: (key, value) => {
      store.set(KEY_PREFIX + key, value)
    },
    remove: (key) => {
      store.delete(KEY_PREFIX + key)
    },
  }
}

/**
 * localStorage 在隐私模式、配额耗尽或非浏览器环境下会直接抛异常。
 * 缓存只是加速手段，读不到就退回内存实现，不该让整个应用起不来。
 */
function browserCache(): KeyValueCache {
  let storage: Storage
  try {
    storage = globalThis.localStorage
    storage.setItem(KEY_PREFIX + '__probe__', '1')
    storage.removeItem(KEY_PREFIX + '__probe__')
  } catch {
    return memoryCache()
  }
  return {
    get: (key) => {
      try {
        return storage.getItem(KEY_PREFIX + key)
      } catch {
        return null
      }
    },
    set: (key, value) => {
      try {
        storage.setItem(KEY_PREFIX + key, value)
      } catch {
        // 配额满或隐私模式：忽略写入，读取方按缓存未命中处理
      }
    },
    remove: (key) => {
      try {
        storage.removeItem(KEY_PREFIX + key)
      } catch {
        // 同上
      }
    },
  }
}

export function createWebPlatform(): Platform {
  return { kind: 'web', cache: browserCache() }
}
