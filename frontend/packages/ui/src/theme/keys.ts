/**
 * 主题的键名与类型。单独成文件是因为首屏脚本要注入到 HTML 里，
 * 它只能拿到这些字面量，不能把 React 模块拖进 Vite 配置。
 */

export type Theme = 'light' | 'dark'

/** 端口层的键名：`Platform.cache` 用的是这个名字。 */
export const THEME_KEY = 'theme'

/**
 * localStorage 里实际落盘的完整键名。
 *
 * 前缀必须与 `packages/platform-web` 的 `KEY_PREFIX` 一致 —— 那里给所有缓存键加前缀，
 * 以避免与同源的其他页面冲突。两处不一致的后果是首屏拿不到用户已保存的偏好、
 * 退化到系统偏好（界面仍然可用，但用户的选择好像没记住）。
 * 这条一致性由 `theme-context.test.tsx` 里的检查兜住，不靠人记住。
 */
export const THEME_STORAGE_PREFIX = 'paideia:'
export const THEME_STORAGE_KEY = THEME_STORAGE_PREFIX + THEME_KEY

/**
 * 主题持久化只需要读写两个操作，所以这里只声明这两个方法。
 * `packages/core` 的 `KeyValueCache`（get/set/remove）在结构上正好满足它，
 * 因此应用可以直接把 `platform.cache` 传进来，而 `packages/ui` 不必依赖 core。
 */
export interface ThemeStorage {
  get(key: string): string | null
  set(key: string, value: string): void
}

export function isTheme(value: string | null | undefined): value is Theme {
  return value === 'light' || value === 'dark'
}
