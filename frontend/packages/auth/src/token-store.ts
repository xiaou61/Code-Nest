/**
 * 令牌存储。
 *
 * <p><b>access 只放内存、refresh 落盘</b>：access 放内存让 XSS 无法跨刷新拿到它；
 * refresh 必须跨刷新与跨重启存活，所以走注入的存储——形状只有 `get`/`set`/`remove`，
 * 与 `packages/core` 的 `KeyValueCache` 结构化匹配，因此应用可以直接把 `platform.cache` 传进来，
 * 本包不必依赖平台实现。
 *
 * <p><b>天花板要说清</b>：refresh 落在 localStorage（Web）或主进程缓存（桌面）里，
 * 两者对 XSS 都是可读的。轮换买到的是"单个 access 寿命很短"与"盗用后可经重用检测发现"，
 * **买不到"XSS 拿不到令牌"**。
 */

export interface TokenStorage {
  get(key: string): string | null
  set(key: string, value: string): void
  remove(key: string): void
}

/** 端口层的键名，与 platform-web 的 `paideia:` 前缀一起构成 localStorage 的完整键。 */
export const REFRESH_TOKEN_KEY = 'refresh-token'

export interface IssuedTokens {
  accessToken: string
  refreshToken: string
}

export interface TokenStore {
  access(): string | null
  refresh(): string | null
  set(tokens: IssuedTokens): void
  clear(): void
}

export function createTokenStore(storage: TokenStorage): TokenStore {
  // 模块闭包里的内存变量：刷新页面即消失，这是刻意的最小暴露面
  let accessToken: string | null = null
  let refreshToken: string | null = storage.get(REFRESH_TOKEN_KEY)

  return {
    access: () => accessToken,
    refresh: () => refreshToken,
    set(tokens) {
      accessToken = tokens.accessToken
      refreshToken = tokens.refreshToken
      storage.set(REFRESH_TOKEN_KEY, tokens.refreshToken)
    },
    clear() {
      accessToken = null
      refreshToken = null
      storage.remove(REFRESH_TOKEN_KEY)
    },
  }
}
