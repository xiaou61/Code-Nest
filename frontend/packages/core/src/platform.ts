export type PlatformKind = 'web' | 'desktop'

export interface KeyValueCache {
  get(key: string): string | null
  set(key: string, value: string): void
  remove(key: string): void
}

/**
 * 平台能力端口。业务代码只依赖这个接口，不直接引用任何宿主 API——
 * Web 与桌面壳各提供一份实现，同一份代码因此可以同时服务两端。
 *
 * 本期只包含骨架确实用到的方法。文件读写、深链、自动更新等能力等出现需求再加，
 * 避免先定义一堆没人实现的方法。
 */
export interface Platform {
  readonly kind: PlatformKind
  readonly cache: KeyValueCache
}
